/* =====================================
   PETCARE - RESERVAS
===================================== */

let resClientes = [];
let resMascotas = [];
let resVeterinarios = [];
let resReservas = [];

function resFechaLocal() {
    const d = new Date();
    const pad = n => String(n).padStart(2, "0");

    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function resLlenarSelect(selector, items, texto, etiqueta) {
    const campo = $(selector);
    const anterior = campo.value;

    campo.replaceChildren(new Option(texto, ""));

    items.forEach(item => {
        campo.add(new Option(etiqueta(item), String(item.id)));
    });

    if (items.some(item => String(item.id) === anterior)) {
        campo.value = anterior;
    } else if (items.length) {
        campo.value = String(items[0].id);
    }
}

async function resPreparar() {
    [resClientes, resVeterinarios, resReservas] =
        await Promise.all([
            api("/api/clientes"),
            api("/api/veterinarios"),
            api("/api/reservas")
        ]);

    resLlenarSelect(
        "#res-cliente",
        resClientes,
        "Seleccionar propietario",
        c => `${c.nombre} (ID ${c.id})`
    );

    resLlenarSelect(
        "#res-veterinario",
        resVeterinarios,
        "Seleccionar veterinario",
        v => `${v.nombre} - ${v.especialidad}`
    );

    const filtro = $("#res-filtro-cliente");
    const anterior = filtro.value;

    filtro.replaceChildren(
        new Option("Todos los clientes", "")
    );

    resClientes.forEach(c => {
        filtro.add(new Option(c.nombre, String(c.id)));
    });

    filtro.value = anterior;

    if (!$("#res-fecha").value) {
        $("#res-fecha").value = resFechaLocal();
    }

    $("#res-fecha").min = resFechaLocal();

    await resCargarMascotas();
    await resCargarHoras();
    resRenderizar();
}

async function resCargarMascotas() {
    const clienteId = $("#res-cliente").value;

    resMascotas = clienteId
        ? await api(`/api/clientes/${clienteId}/mascotas`)
        : [];

    resLlenarSelect(
        "#res-mascota",
        resMascotas,
        "Seleccionar mascota",
        m => `${m.nombre} (${m.especie})`
    );
}

async function resCargarHoras() {
    const veterinarioId = $("#res-veterinario").value;
    const fecha = $("#res-fecha").value;
    const selector = $("#res-hora");

    selector.replaceChildren(
        new Option("Seleccionar hora", "")
    );

    if (!veterinarioId || !fecha) {
        $("#res-disponibilidad").textContent =
            "Selecciona un veterinario y una fecha.";
        return;
    }

    try {
        const respuesta = await api(
            `/api/veterinarios/${veterinarioId}/disponibilidad` +
            `?fecha=${encodeURIComponent(fecha)}`
        );

        const horas = respuesta.horasDisponibles || [];

        horas.forEach(h => {
            selector.add(new Option(h.slice(0, 5), h));
        });

        if (horas.length) {
            selector.value = horas[0];
        }

        $("#res-disponibilidad").textContent =
            horas.length
                ? `${horas.length} horarios disponibles para esa fecha.`
                : "No hay horarios disponibles para esa fecha.";

    } catch (error) {
        $("#res-disponibilidad").textContent =
            "No fue posible consultar la disponibilidad.";
        mensaje(error.message, true);
    }
}

function resNombre(lista, id) {
    return lista.find(x => x.id === id)?.nombre || `ID ${id}`;
}

function resRenderizar() {
    const filtro = $("#res-filtro-cliente").value;

    const visibles = resReservas.filter(r =>
        !filtro || String(r.clienteId) === filtro
    );

    $("#res-total").textContent =
        `${visibles.length} reservas`;

    $("#res-tabla").innerHTML = visibles.length
        ? visibles.map(r => {
            const puedeEditar =
                r.estado === "PENDIENTE" ||
                r.estado === "CONFIRMADA";

            return `
                <tr>
                    <td>${r.id}</td>
                    <td>${escapar(resNombre(resClientes, r.clienteId))}</td>
                    <td>ID ${r.mascotaId}</td>
                    <td>${escapar(resNombre(resVeterinarios, r.veterinarioId))}</td>
                    <td>${escapar(r.fecha)}</td>
                    <td>${escapar(r.hora.slice(0, 5))}</td>
                    <td><span class="estado-reserva">${escapar(r.estado)}</span></td>
                    <td>
                        ${r.estado === "PENDIENTE" ? `
                            <button class="boton principal pequeno"
                                    data-accion="confirmar"
                                    data-id="${r.id}">
                                Confirmar
                            </button>
                        ` : ""}

                        ${puedeEditar ? `
                            <button class="boton secundario pequeno"
                                    data-accion="reprogramar"
                                    data-id="${r.id}">
                                Reprogramar
                            </button>
                            <button class="boton peligro pequeno"
                                    data-accion="cancelar"
                                    data-id="${r.id}">
                                Cancelar
                            </button>
                        ` : ""}
                    </td>
                </tr>
            `;
        }).join("")
        : '<tr><td colspan="8">No hay reservas registradas.</td></tr>';
}

function resLimpiar() {
    $("#res-form").reset();
    $("#res-edicion-id").value = "";
    $("#res-cliente").disabled = false;
    $("#res-mascota").disabled = false;
    $("#res-motivo").disabled = false;
    $("#res-titulo").textContent = "Registrar reserva";
    $("#res-guardar").textContent = "Registrar reserva";
    $("#res-fecha").value = resFechaLocal();
}

async function resActualizarListado() {
    resReservas = await api("/api/reservas");
    resRenderizar();
    await resCargarHoras();
}

$("#res-cliente").addEventListener("change", async () => {
    try {
        await resCargarMascotas();
    } catch (error) {
        mensaje(error.message, true);
    }
});

$("#res-veterinario").addEventListener(
    "change", resCargarHoras
);

$("#res-fecha").addEventListener(
    "change", resCargarHoras
);

$("#res-filtro-cliente").addEventListener(
    "change", resRenderizar
);

$("#res-limpiar").addEventListener("click", async () => {
    resLimpiar();
    await resPreparar().catch(e => mensaje(e.message, true));
});

$("#res-form").addEventListener("submit", async evento => {
    evento.preventDefault();

    const id = $("#res-edicion-id").value;

    const veterinarioId = Number($("#res-veterinario").value);
    const fecha = $("#res-fecha").value;
    const hora = $("#res-hora").value;

    if (!hora) {
        mensaje("Selecciona una hora disponible.", true);
        return;
    }

    const datos = id
        ? { veterinarioId, fecha, hora }
        : {
            clienteId: Number($("#res-cliente").value),
            mascotaId: Number($("#res-mascota").value),
            veterinarioId,
            fecha,
            hora,
            motivo: $("#res-motivo").value.trim()
        };

    try {
        await api(
            id ? `/api/reservas/${id}` : "/api/reservas",
            id ? "PUT" : "POST",
            datos
        );

        resLimpiar();
        await resPreparar();

        mensaje(
            id
                ? "Reserva reprogramada. Su estado vuelve a PENDIENTE."
                : "Reserva registrada correctamente."
        );

    } catch (error) {
        mensaje(error.message, true);
    }
});

$("#res-tabla").addEventListener("click", async evento => {
    const boton = evento.target.closest("[data-accion]");
    if (!boton) return;

    const id = Number(boton.dataset.id);
    const reserva = resReservas.find(r => r.id === id);
    if (!reserva) return;

    const accion = boton.dataset.accion;

    try {
        if (accion === "reprogramar") {
            $("#res-edicion-id").value = reserva.id;

            $("#res-cliente").value = String(reserva.clienteId);
            await resCargarMascotas();

            $("#res-mascota").value = String(reserva.mascotaId);
            $("#res-veterinario").value = String(reserva.veterinarioId);
            $("#res-fecha").value = reserva.fecha;
            $("#res-motivo").value = reserva.motivo;

            $("#res-cliente").disabled = true;
            $("#res-mascota").disabled = true;
            $("#res-motivo").disabled = true;

            $("#res-titulo").textContent =
                `Reprogramar reserva #${id}`;

            $("#res-guardar").textContent =
                "Guardar reprogramación";

            await resCargarHoras();

            $("#res-form").scrollIntoView({
                behavior: "smooth"
            });

            return;
        }

        let nuevoEstado;

        if (accion === "confirmar") {
            nuevoEstado = "CONFIRMADA";
        } else if (accion === "cancelar") {
            if (!confirm(`¿Cancelar la reserva #${id}?`)) return;
            nuevoEstado = "CANCELADA";
        } else {
            return;
        }

        await api(
            `/api/reservas/${id}/estado`,
            "PATCH",
            { estado: nuevoEstado }
        );

        await resActualizarListado();

        mensaje(`Reserva #${id}: ${nuevoEstado}.`);

    } catch (error) {
        mensaje(error.message, true);
    }
});

$('.menu-item[data-vista="reservas"]')
    .addEventListener("click", () => {
        resPreparar().catch(error => {
            mensaje(error.message, true);
        });
    });
