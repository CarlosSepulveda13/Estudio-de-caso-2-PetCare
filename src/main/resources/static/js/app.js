
const $ = (selector) => document.querySelector(selector);

let clientes = [];
let mascotas = [];

// ----------------------------
// Comunicacion con Spring Boot
// ----------------------------
async function api(ruta, metodo = "GET", datos = null) {
    const opciones = { method: metodo };

    if (datos !== null) {
        opciones.headers = {
            "Content-Type": "application/json"
        };
        opciones.body = JSON.stringify(datos);
    }

    const respuesta = await fetch(ruta, opciones);

    const resultado = await respuesta.json()
        .catch(() => null);

    if (!respuesta.ok) {
        throw new Error(
            resultado?.detail ||
            resultado?.message ||
            resultado?.error ||
            `Error HTTP ${respuesta.status}`
        );
    }

    return resultado;
}

function mensaje(texto, error = false) {
    const elemento = $("#mensaje");
    elemento.textContent = texto;
    elemento.classList.toggle("error", error);
    elemento.hidden = false;
}

function escapar(valor) {
    return String(valor ?? "").replace(
        /[&<>"']/g,
        (caracter) => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[caracter]
    );
}

// ----------------------------
// Navegacion
// ----------------------------
function cambiarVista(nombre) {
    document.querySelectorAll(".vista").forEach((vista) => {
        vista.hidden = vista.id !== `vista-${nombre}`;
    });

    document.querySelectorAll(".menu-item").forEach((boton) => {
        boton.classList.toggle(
            "activo",
            boton.dataset.vista === nombre
        );
    });

    $("#mensaje").hidden = true;
}

// ----------------------------
// Gestion de clientes
// ----------------------------
async function cargarClientes() {
    clientes = await api("/api/clientes");
    renderizarClientes();

    const selector = $("#mascota-cliente-select");
    const anterior = selector.value;

    selector.replaceChildren();

    const inicial = new Option(
        "Selecciona un cliente",
        ""
    );
    selector.add(inicial);

    clientes.forEach((cliente) => {
        selector.add(
            new Option(
                `${cliente.nombre} (ID ${cliente.id})`,
                String(cliente.id)
            )
        );
    });

    if (clientes.some(c => String(c.id) === anterior)) {
        selector.value = anterior;
    } else if (clientes.length > 0) {
        selector.value = String(clientes[0].id);
    }

    await cargarMascotas();
}

function renderizarClientes() {
    const tabla = $("#tabla-clientes");

    $("#total-clientes").textContent =
        `${clientes.length} clientes`;

    if (clientes.length === 0) {
        tabla.innerHTML = `
            <tr>
                <td colspan="5">
                    Todavia no hay clientes registrados.
                </td>
            </tr>`;
        return;
    }

    tabla.innerHTML = clientes.map((cliente) => `
        <tr>
            <td>${cliente.id}</td>
            <td>${escapar(cliente.nombre)}</td>
            <td>${escapar(cliente.correo)}</td>
            <td>${escapar(cliente.telefono || "-")}</td>
            <td>
                <button class="boton secundario pequeno"
                        data-accion="editar-cliente"
                        data-id="${cliente.id}">
                    Editar
                </button>

                <button class="boton peligro pequeno"
                        data-accion="eliminar-cliente"
                        data-id="${cliente.id}">
                    Desactivar
                </button>
            </td>
        </tr>
    `).join("");
}

function limpiarCliente() {
    $("#form-cliente").reset();
    $("#cliente-id").value = "";
    $("#titulo-form-cliente").textContent =
        "Registrar cliente";
}

async function guardarCliente(evento) {
    evento.preventDefault();

    const formulario = evento.currentTarget;
    const datos = Object.fromEntries(
        new FormData(formulario)
    );

    const id = $("#cliente-id").value;

    try {
        const resultado = await api(
            id ? `/api/clientes/${id}` : "/api/clientes",
            id ? "PUT" : "POST",
            datos
        );

        limpiarCliente();
        await cargarClientes();

        if (!id && resultado?.id) {
            $("#mascota-cliente-select").value =
                String(resultado.id);
            await cargarMascotas();
        }

        mensaje(
            id
                ? "Cliente actualizado correctamente."
                : "Cliente registrado correctamente."
        );

    } catch (error) {
        mensaje(error.message, true);
    }
}

async function accionCliente(evento) {
    const boton = evento.target.closest("[data-accion]");
    if (!boton) return;

    const id = Number(boton.dataset.id);
    const cliente = clientes.find(c => c.id === id);

    if (!cliente) return;

    if (boton.dataset.accion === "editar-cliente") {
        const form = $("#form-cliente");

        $("#cliente-id").value = cliente.id;
        form.elements.nombre.value = cliente.nombre;
        form.elements.correo.value = cliente.correo;
        form.elements.telefono.value =
            cliente.telefono || "";

        $("#titulo-form-cliente").textContent =
            "Editar cliente";

        form.scrollIntoView({ behavior: "smooth" });
    }

    if (boton.dataset.accion === "eliminar-cliente") {
        const confirmar = window.confirm(
            `¿Desactivar a ${cliente.nombre}?\n` +
            "Sus datos historicos se conservaran."
        );

        if (!confirmar) return;

        try {
            await api(`/api/clientes/${id}`, "DELETE");
            limpiarCliente();
            await cargarClientes();

            mensaje("Cliente desactivado correctamente.");

        } catch (error) {
            mensaje(error.message, true);
        }
    }
}

// ----------------------------
// Gestion de mascotas
// ----------------------------
async function cargarMascotas() {
    const clienteId = $("#mascota-cliente-select").value;

    limpiarMascota();

    if (!clienteId) {
        mascotas = [];
        renderizarMascotas();
        return;
    }

    mascotas = await api(
        `/api/clientes/${clienteId}/mascotas`
    );

    renderizarMascotas();
}

function renderizarMascotas() {
    const tabla = $("#tabla-mascotas");

    $("#total-mascotas").textContent =
        `${mascotas.length} mascotas`;

    if (mascotas.length === 0) {
        tabla.innerHTML = `
            <tr>
                <td colspan="7">
                    No hay mascotas para este propietario.
                </td>
            </tr>`;
        return;
    }

    tabla.innerHTML = mascotas.map((mascota) => `
        <tr>
            <td>${mascota.id}</td>
            <td>${escapar(mascota.nombre)}</td>
            <td>${escapar(mascota.especie)}</td>
            <td>${escapar(mascota.raza)}</td>
            <td>${escapar(mascota.fechaNacimiento)}</td>
            <td>${escapar(mascota.peso)} kg</td>
            <td>
                <button class="boton secundario pequeno"
                        data-accion="editar-mascota"
                        data-id="${mascota.id}">
                    Editar
                </button>

                <button class="boton peligro pequeno"
                        data-accion="eliminar-mascota"
                        data-id="${mascota.id}">
                    Desactivar
                </button>
            </td>
        </tr>
    `).join("");
}

function limpiarMascota() {
    $("#form-mascota").reset();
    $("#mascota-id").value = "";

    $("#titulo-form-mascota").textContent =
        "Registrar mascota";
}

async function guardarMascota(evento) {
    evento.preventDefault();

    const clienteId = $("#mascota-cliente-select").value;

    if (!clienteId) {
        mensaje("Primero selecciona un propietario.", true);
        return;
    }

    const formulario = evento.currentTarget;

    const datos = Object.fromEntries(
        new FormData(formulario)
    );

    datos.peso = Number(datos.peso);

    const id = $("#mascota-id").value;

    const ruta = id
        ? `/api/mascotas/${id}`
        : `/api/clientes/${clienteId}/mascotas`;

    try {
        await api(ruta, id ? "PUT" : "POST", datos);

        await cargarMascotas();

        mensaje(
            id
                ? "Mascota actualizada correctamente."
                : "Mascota registrada correctamente."
        );

    } catch (error) {
        mensaje(error.message, true);
    }
}

async function accionMascota(evento) {
    const boton = evento.target.closest("[data-accion]");
    if (!boton) return;

    const id = Number(boton.dataset.id);
    const mascota = mascotas.find(m => m.id === id);

    if (!mascota) return;

    if (boton.dataset.accion === "editar-mascota") {
        const form = $("#form-mascota");

        $("#mascota-id").value = mascota.id;
        form.elements.nombre.value = mascota.nombre;
        form.elements.especie.value = mascota.especie;
        form.elements.raza.value = mascota.raza;

        form.elements.fechaNacimiento.value =
            mascota.fechaNacimiento;

        form.elements.peso.value = mascota.peso;

        $("#titulo-form-mascota").textContent =
            "Editar mascota";

        form.scrollIntoView({ behavior: "smooth" });
    }

    if (boton.dataset.accion === "eliminar-mascota") {
        const confirmar = window.confirm(
            `¿Desactivar a ${mascota.nombre}?\n` +
            "Su historial no sera eliminado."
        );

        if (!confirmar) return;

        try {
            await api(`/api/mascotas/${id}`, "DELETE");

            await cargarMascotas();

            mensaje("Mascota desactivada correctamente.");

        } catch (error) {
            mensaje(error.message, true);
        }
    }
}

// ----------------------------
// Inicio de la aplicacion
// ----------------------------
document.querySelectorAll(".menu-item").forEach(boton => {
    boton.addEventListener("click", () => {
        cambiarVista(boton.dataset.vista);
    });
});

$("#form-cliente").addEventListener(
    "submit",
    guardarCliente
);

$("#cancelar-cliente").addEventListener(
    "click",
    limpiarCliente
);

$("#tabla-clientes").addEventListener(
    "click",
    accionCliente
);

$("#form-mascota").addEventListener(
    "submit",
    guardarMascota
);

$("#cancelar-mascota").addEventListener(
    "click",
    limpiarMascota
);

$("#tabla-mascotas").addEventListener(
    "click",
    accionMascota
);

$("#mascota-cliente-select").addEventListener(
    "change",
    async () => {
        try {
            await cargarMascotas();
        } catch (error) {
            mensaje(error.message, true);
        }
    }
);

cargarClientes().catch(error => {
    mensaje(
        "No fue posible conectar con Spring Boot: " +
        error.message,
        true
    );
});
