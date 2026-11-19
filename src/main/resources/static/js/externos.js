/* =====================================
   PETCARE - MEDICAMENTOS Y UBICACION
===================================== */

// Consultar medicamentos
$("#ext-form-medicamentos").addEventListener(
    "submit",
    async evento => {
        evento.preventDefault();

        const nombre = $("#ext-nombre").value.trim();

        if (!nombre) {
            mensaje("Ingresa un medicamento.", true);
            return;
        }

        const tabla = $("#ext-med-tabla");
        const estado = $("#ext-med-estado");

        tabla.replaceChildren();
        estado.textContent = "Buscando medicamentos...";

        try {
            const respuesta = await api(
                "/api/externo/medicamentos?nombre=" +
                encodeURIComponent(nombre)
            );

            const medicamentos = respuesta.medicamentos || [];

            estado.textContent =
                `Resultados encontrados: ${medicamentos.length}`;

            if (medicamentos.length === 0) {
                tabla.innerHTML = `
                    <tr>
                        <td colspan="3">
                            No se encontraron medicamentos.
                        </td>
                    </tr>
                `;
                return;
            }

            tabla.innerHTML = medicamentos.map(m => `
                <tr>
                    <td>${escapar(m.nombre)}</td>
                    <td>${escapar(m.categoria)}</td>
                    <td>${escapar(m.advertencia)}</td>
                </tr>
            `).join("");

        } catch (error) {
            estado.textContent =
                "No fue posible consultar los medicamentos.";

            mensaje(
                "Verifica que la API externa esté funcionando. " +
                error.message,
                true
            );
        }
    }
);


// Consultar ubicación de la clínica
async function extCargarUbicacion() {
    const info = $("#ext-ubicacion-info");

    info.textContent = "Consultando ubicación...";

    try {
        const ubicacion = await api("/api/externo/ubicacion");

        const lat = Number(ubicacion.latitud);
        const lon = Number(ubicacion.longitud);

        if (!Number.isFinite(lat) ||
            !Number.isFinite(lon) ||
            Math.abs(lat) > 90 ||
            Math.abs(lon) > 180) {
            throw new Error("Coordenadas inválidas.");
        }

        info.replaceChildren();

        const nombre = document.createElement("h4");
        nombre.textContent = ubicacion.nombre || "Veterinaria";

        const direccion = document.createElement("p");
        direccion.textContent =
            ubicacion.direccion || "Dirección no disponible";

        const coordenadas = document.createElement("p");
        coordenadas.textContent =
            `Coordenadas: ${lat}, ${lon}`;

        info.append(nombre, direccion, coordenadas);

        // Área mostrada alrededor de las coordenadas.
        const margen = 0.005;

        const bbox = [
            lon - margen,
            lat - margen,
            lon + margen,
            lat + margen
        ].join(",");

        const urlMapa =
            "https://www.openstreetmap.org/export/embed.html" +
            "?bbox=" + encodeURIComponent(bbox) +
            "&layer=mapnik" +
            "&marker=" + encodeURIComponent(`${lat},${lon}`);

        $("#ext-mapa").src = urlMapa;

        const enlace = $("#ext-mapa-enlace");

        enlace.href =
            "https://www.openstreetmap.org/" +
            "?mlat=" + encodeURIComponent(lat) +
            "&mlon=" + encodeURIComponent(lon) +
            "#map=17/" + lat + "/" + lon;

        enlace.hidden = false;

    } catch (error) {
        info.textContent =
            "No fue posible obtener la ubicación de la clínica.";

        mensaje(error.message, true);
    }
}


// Cargar la ubicación al abrir la pantalla
$('.menu-item[data-vista="externos"]')
    .addEventListener("click", extCargarUbicacion);
