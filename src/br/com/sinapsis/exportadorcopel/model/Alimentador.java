package br.com.sinapsis.exportadorcopel.model;

import java.util.ArrayList;
import java.util.List;

public class Alimentador {

	private int codigo;
	private String nome;
	private String sigla;
	private List<Medicao> medicoes;
	
	public Alimentador() {
		medicoes = new ArrayList<>();
	}
	
	public int getCodigo() {
		return codigo;
	}
	
	public void setCodigo(int codigo) {
		this.codigo = codigo;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getSigla() {
		return sigla;
	}

	public void setSigla(String sigla) {
		this.sigla = sigla;
	}

	public List<Medicao> getMedicoes() {
		return medicoes;
	}

	public void setMedicoes(List<Medicao> medicoes) {
		this.medicoes = medicoes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + codigo;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Alimentador other = (Alimentador) obj;
		if (codigo != other.codigo)
			return false;
		return true;
	}
	
}
