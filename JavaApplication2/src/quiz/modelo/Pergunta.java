package quiz.modelo;

import java.io.Serializable;

/**
 * Representa uma pergunta do quiz com 4 alternativas.
 * Implementa Serializable para trafegar pela rede via ObjectOutputStream.
 */
public class Pergunta implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String enunciado;
    private final String[] alternativas; // A, B, C, D
    private final int indiceCorreto;     // 0=A, 1=B, 2=C, 3=D

    public Pergunta(String enunciado, String[] alternativas, int indiceCorreto) {
        if (alternativas.length != 4)
            throw new IllegalArgumentException("Uma pergunta precisa ter exatamente 4 alternativas.");
        this.enunciado = enunciado;
        this.alternativas = alternativas;
        this.indiceCorreto = indiceCorreto;
    }

    public String getEnunciado() { return enunciado; }
    public String[] getAlternativas() { return alternativas; }
    public int getIndiceCorreto() { return indiceCorreto; }

    public boolean verificarResposta(int indice) {
        return indice == indiceCorreto;
    }

    public String getLetraCorreta() {
        return String.valueOf((char) ('A' + indiceCorreto));
    }

    @Override
    public String toString() {
        return "Pergunta{enunciado='" + enunciado + "', correta=" + getLetraCorreta() + "}";
    }
}