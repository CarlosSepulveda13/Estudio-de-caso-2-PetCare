/* =====================================
   PETCARE - ATENCIONES MEDICAS
===================================== */

let ateReservasValidas = [];

function ateOpciones(selector, items, texto, etiqueta) {
    const campo = $(selector);

    campo.replaceChildren(new Option(texto, ""));

    items.forEach(item => {
        campo.add(new Option(etiqueta(item), String(item.id)));
    });

    if (items.length) {
        campo.value = String(items[0].id);
    }
}

async function atePreparar() {
    const clientesActivos = await api("/api/clientes");

    ateOpciones(
        "#ate-cliente",
        clientesActivos,
        "Seleccionar propietario",
        c => c.nombre
    );

    await ateCargarMascotas();
}

async function ateCargarMascotas() {
    const clienteId = $("#ate-cliente").value;

    const lista = clienteId
        ? await api(`/api/clientes/${clienteId}/mascotas`)
        : [];

    ateOpciones(
        "#ate-mascota",
        lista,
        "Seleccionar mascota",
        m => `${m.nombre} (${m.especie})`
    );

    await ateCargarHistorial();
}

async function ateCargarHistorial() {
    const mascotaId = $("#ate-mascota").value;
    const contenedor = $("#ate-historial");

    ateReservasValidas = [];

    if (!mascotaId) {
        $("#ate-reserva").replaceChildren(
            new Option("Seleccionar reserva", "")
        );

        contenedor.textContent =
            "Selecciona una mascota para consultar su historial.";

        $("#ate-guardar").disabled = true;
        return;
    }

    const [reservas, historial] = await Promise.all([
        api(`/api/mascotas/${mascotaId}/reservas`),
        api(`/api/mascotas/${mascotaId}/historial`)
    ]);

    // Mostrar todas las reservas confirmadas,
// incluso las que todavía no han comenzado.
    ateReservasValidas = reservas.filter(
        r => r.estado === "CONFIRMADA"
    );

    ateOpciones(
        "#ate-reserva",
        ateReservasValidas,
        "Seleccionar reserva confirmada",
        r => {
            const fechaHora = new Date(`${r.fecha}T${r.hora}`);
            const disponible = fechaHora <= new Date();

            const estado = disponible
                ? "Lista para atender"
                : "Próxima cita";

            return `#${r.id} - ${r.fecha} ${r.hora.slice(0, 5)} (${estado})`;
        }
    );

    ateActualizarPermiso();
    ateRenderizarHistorial(historial);
}

function ateActualizarPermiso() {
    const reservaId = Number($("#ate-reserva").value);

    const reserva = ateReservasValidas.find(
        r => r.id === reservaId
    );

    const boton = $("#ate-guardar");
    const ayuda = $("#ate-ayuda");

    if (!reserva) {
        boton.disabled = true;
        ayuda.textContent =
            "No hay reservas confirmadas para esta mascota.";
        return;
    }

    const fechaHora = new Date(
        `${reserva.fecha}T${reserva.hora}`
    );

    if (fechaHora > new Date()) {
        boton.disabled = true;

        ayuda.textContent =
            `La reserva #${reserva.id} está confirmada, ` +
            `pero solo podrás registrar su atención desde ` +
            `${reserva.fecha} a las ${reserva.hora.slice(0, 5)}.`;
    } else {
        boton.disabled = false;

        ayuda.textContent =
            `La reserva #${reserva.id} está lista ` +
            `para registrar su atención médica.`;
    }
}

$("#ate-reserva").addEventListener(
    "change",
    ateActualizarPermiso
);


function ateRenderizarHistorial(historial) {
    const contenedor = $("#ate-historial");

    if (!historial.length) {
        contenedor.innerHTML =
            '<p class="texto-ayuda">Esta mascota aún no tiene atenciones registradas.</p>';
        return;
    }

    contenedor.innerHTML = historial.map(a => {

        const medicamentos = (a.medicamentos || [])
            .map(m => `
                <li>
                    <strong>${escapar(m.nombre)}</strong>:
                    ${escapar(m.dosis)},
                    ${escapar(m.frecuencia)},
                    durante ${escapar(m.duracionDias)} días.
                    ${m.indicaciones
                ? escapar(m.indicaciones)
                : ""}
                </li>
            `).join("");

        return `
            <article class="historial-item">

                <h4>Atención #${a.id}</h4>

                <p>
                    <strong>Fecha:</strong>
                    ${escapar(a.fechaAtencion?.replace("T", " ") || "-")}
                </p>

                <p>
                    <strong>Reserva:</strong> #${a.reservaId}
                    · <strong>Veterinario:</strong> #${a.veterinarioId}
                </p>

                <p>
                    <strong>Diagnóstico:</strong>
                    ${escapar(a.diagnostico)}
                </p>

                <p>
                    <strong>Tratamiento:</strong>
                    ${escapar(a.tratamiento)}
                </p>

                <p><strong>Medicamentos prescritos:</strong></p>

                ${medicamentos
            ? `<ul>${medicamentos}</ul>`
            : '<p>Sin medicamentos prescritos.</p>'}

            </article>
        `;
    }).join("");
}

$("#ate-cliente").addEventListener("change", async () => {
    try {
        await ateCargarMascotas();
    } catch (error) {
        mensaje(error.message, true);
    }
});

$("#ate-mascota").addEventListener("change", async () => {
    try {
        await ateCargarHistorial();
    } catch (error) {
        mensaje(error.message, true);
    }
});

$("#ate-actualizar").addEventListener("click", async () => {
    try {
        await ateCargarHistorial();
        mensaje("Historial actualizado.");
    } catch (error) {
        mensaje(error.message, true);
    }
});

$("#ate-form").addEventListener("submit", async evento => {
    evento.preventDefault();

    const reservaId = Number($("#ate-reserva").value);
    const mascotaId = Number($("#ate-mascota").value);

    const reserva = ateReservasValidas.find(
        r => r.id === reservaId
    );

    if (!reserva) {
        mensaje(
            "Selecciona una reserva confirmada que ya haya comenzado.",
            true
        );
        return;
    }
    const fechaHora = new Date(
        `${reserva.fecha}T${reserva.hora}`
    );

    if (fechaHora > new Date()) {
        mensaje(
            "Esta reserva todavía no ha comenzado. " +
            "No se puede registrar su atención médica.",
            true
        );
        return;
    }

    const medicamentos = [];

    const nombre = $("#ate-med-nombre").value.trim();
    const dosis = $("#ate-med-dosis").value.trim();
    const frecuencia = $("#ate-med-frecuencia").value.trim();
    const duracion = Number($("#ate-med-duracion").value);

    if (nombre) {
        if (!dosis || !frecuencia || duracion < 1) {
            mensaje(
                "Completa dosis, frecuencia y duración del medicamento.",
                true
            );
            return;
        }

        medicamentos.push({
            nombre,
            dosis,
            frecuencia,
            duracionDias: duracion,
            indicaciones: $("#ate-med-indicaciones").value.trim()
        });
    }

    const datos = {
        reservaId,
        veterinarioId: reserva.veterinarioId,
        diagnostico: $("#ate-diagnostico").value.trim(),
        tratamiento: $("#ate-tratamiento").value.trim(),
        medicamentos
    };

    try {
        await api(
            `/api/mascotas/${mascotaId}/atenciones`,
            "POST",
            datos
        );

        $("#ate-form").reset();

        await ateCargarHistorial();

        mensaje(
            "Atención médica registrada y reserva marcada como ATENDIDA."
        );

    } catch (error) {
        mensaje(error.message, true);
    }
});

$('.menu-item[data-vista="atenciones"]')
    .addEventListener("click", () => {
        atePreparar().catch(error => {
            mensaje(error.message, true);
        });
    });
