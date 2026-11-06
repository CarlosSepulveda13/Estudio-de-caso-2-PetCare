package cl.ubb.petcare.model;

import jakarta.persistence.*;

@Entity
@Table(name = "medicamentos_prescritos")
public class MedicamentoPrescrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atencion_id", nullable = false)
    private AtencionMedica atencion;

    @Column(nullable = false, length = 150)
    private String nombreMedicamento;

    @Column(nullable = false, length = 100)
    private String dosis;

    @Column(nullable = false, length = 100)
    private String frecuencia;

    @Column(nullable = false)
    private Integer duracionDias;

    @Column(length = 500)
    private String indicaciones;

    public MedicamentoPrescrito() {}

    public Long getId() { return id; }

    public AtencionMedica getAtencion() { return atencion; }
    public void setAtencion(AtencionMedica atencion) {
        this.atencion = atencion;
    }

    public String getNombreMedicamento() {
        return nombreMedicamento;
    }
    public void setNombreMedicamento(String nombreMedicamento) {
        this.nombreMedicamento = nombreMedicamento;
    }

    public String getDosis() { return dosis; }
    public void setDosis(String dosis) { this.dosis = dosis; }

    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) {
        this.frecuencia = frecuencia;
    }

    public Integer getDuracionDias() { return duracionDias; }
    public void setDuracionDias(Integer duracionDias) {
        this.duracionDias = duracionDias;
    }

    public String getIndicaciones() { return indicaciones; }
    public void setIndicaciones(String indicaciones) {
        this.indicaciones = indicaciones;
    }
}

