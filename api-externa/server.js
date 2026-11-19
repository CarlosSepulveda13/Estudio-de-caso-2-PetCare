require("dotenv").config();

const express = require("express");
const cors = require("cors");
const nodemailer = require("nodemailer");

const app = express();

app.use(cors());
app.use(express.json());

const PORT = Number(process.env.PORT || 3001);

// Datos de demostracion para consultar medicamentos.
// No sustituyen una referencia farmacologica profesional.
const medicamentos = [
    {
        nombre: "Amoxicilina",
        categoria: "Antibiotico",
        advertencia: "Requiere indicacion veterinaria."
    },
    {
        nombre: "Meloxicam",
        categoria: "Antiinflamatorio",
        advertencia: "Requiere evaluacion veterinaria."
    },
    {
        nombre: "Carprofeno",
        categoria: "Antiinflamatorio",
        advertencia: "Requiere indicacion veterinaria."
    }
];

// RF-11: Consulta de medicamentos
app.get("/api/medicamentos", (req, res) => {
    const nombre = String(req.query.nombre || "")
        .trim()
        .toLowerCase();

    if (!nombre) {
        return res.status(400).json({
            error: "Debes indicar el nombre del medicamento"
        });
    }

    const resultados = medicamentos.filter(m =>
        m.nombre.toLowerCase().includes(nombre)
    );

    res.json({
        total: resultados.length,
        medicamentos: resultados
    });
});

// RF-12: Ubicacion de la clinica
app.get("/api/clinica/ubicacion", (req, res) => {
    const latitud = Number(process.env.CLINICA_LATITUD);
    const longitud = Number(process.env.CLINICA_LONGITUD);

    if (!process.env.CLINICA_LATITUD ||
        !process.env.CLINICA_LONGITUD ||
        !Number.isFinite(latitud) ||
        !Number.isFinite(longitud) ||
        Math.abs(latitud) > 90 ||
        Math.abs(longitud) > 180) {
        return res.status(503).json({
            error: "Ubicacion de la clinica no configurada"
        });
    }

    res.json({
        nombre: process.env.CLINICA_NOMBRE,
        direccion: process.env.CLINICA_DIRECCION,
        latitud,
        longitud
    });
});


/* RF-13: Notificaciones de reservas */

app.post("/api/notificaciones", async (req, res) => {

    // Validar que la solicitud este autorizada.
    const clave = process.env.API_KEY;

    if (!clave) {
        return res.status(503).json({
            error: "Clave de API no configurada"
        });
    }

    if (req.header("x-api-key") !== clave) {
        return res.status(401).json({
            error: "No autorizado"
        });
    }

    const {
        destinatario,
        tipo,
        reservaId,
        fecha,
        hora
    } = req.body;

    if (
        typeof destinatario !== "string" ||
        !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(destinatario) ||
        !["CONFIRMADA", "CANCELADA"].includes(tipo) ||
        !Number.isInteger(reservaId) ||
        reservaId <= 0 ||
        !fecha ||
        !hora
    ) {
        return res.status(400).json({
            error: "Datos de notificacion invalidos"
        });
    }

    const modoPrueba =
        process.env.CORREO_MODO_PRUEBA === "true";

    try {

        let transporte;
        let remitente;

        if (modoPrueba) {

            // Crear una cuenta SMTP de demostracion.
            const cuenta = await nodemailer.createTestAccount();

            transporte = nodemailer.createTransport({
                host: "smtp.ethereal.email",
                port: 587,
                secure: false,
                auth: {
                    user: cuenta.user,
                    pass: cuenta.pass
                }
            });

            remitente = cuenta.user;

        } else {

            // Configuracion para envios reales.
            if (
                !process.env.SMTP_HOST ||
                !process.env.SMTP_USER ||
                !process.env.SMTP_PASS
            ) {
                return res.status(503).json({
                    error: "Configuracion SMTP incompleta"
                });
            }

            const puerto = Number(
                process.env.SMTP_PORT || 587
            );

            transporte = nodemailer.createTransport({
                host: process.env.SMTP_HOST,
                port: puerto,
                secure: puerto === 465,
                auth: {
                    user: process.env.SMTP_USER,
                    pass: process.env.SMTP_PASS
                }
            });

            remitente = process.env.SMTP_USER;
        }

        const asunto = `PetCare - Reserva ${tipo}`;

        const mensaje =
            `Estimado cliente:\n\n` +
            `Su reserva veterinaria ha sido ${tipo}.\n\n` +
            `Numero de reserva: ${reservaId}\n` +
            `Fecha: ${fecha}\n` +
            `Hora: ${hora}\n\n` +
            `Atentamente,\nEquipo PetCare`;

        const resultado = await transporte.sendMail({
            from: `PetCare <${remitente}>`,
            to: destinatario,
            subject: asunto,
            text: mensaje
        });

        if (modoPrueba) {

            const vistaPrevia =
                nodemailer.getTestMessageUrl(resultado);

            console.log("Correo de prueba:", vistaPrevia);

            return res.json({
                modo: "PRUEBA",
                capturadoEnBuzonPrueba: true,
                entregadoAlDestinatarioReal: false,
                vistaPrevia: vistaPrevia
            });
        }

        return res.json({
            modo: "REAL",
            enviadoAlServidorSMTP: true,
            mensajeId: resultado.messageId
        });

    } catch (error) {

        console.error(
            "Error al enviar correo:",
            error.message
        );

        return res.status(502).json({
            error: "No fue posible procesar el correo"
        });
    }
});


app.listen(PORT, "0.0.0.0", () => {
    console.log(`API externa PetCare en puerto ${PORT}`);
});
