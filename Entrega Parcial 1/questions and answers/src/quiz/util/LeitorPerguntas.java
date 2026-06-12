package quiz.util;


import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import quiz.modelo.Pergunta;



public class LeitorPerguntas {

    public static List<Pergunta> carregar(String caminho) throws IOException {
        List<Pergunta> perguntas = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
            String linha;
            String enunciado = null;
            String[] alternativas = new String[4];
            int indiceCorreto = -1;

            
            while ((linha = br.readLine()) != null) {
                linha = linha.trim();

                if (linha.isEmpty() || linha.startsWith("#")) continue;

                
                
                if (linha.startsWith("PERGUNTA:")) {
                    enunciado = linha.substring("PERGUNTA:".length()).trim();
                    indiceCorreto = -1;
                    alternativas = new String[4];

                } else if (linha.startsWith("A:")) {
                    alternativas[0] = linha.substring(2).trim();
                } else if (linha.startsWith("B:")) {
                    alternativas[1] = linha.substring(2).trim();
                } else if (linha.startsWith("C:")) {
                    alternativas[2] = linha.substring(2).trim();
                } else if (linha.startsWith("D:")) {
                    alternativas[3] = linha.substring(2).trim();

                    
                    
                } else if (linha.startsWith("RESPOSTA:")) {
                    String letra = linha.substring("RESPOSTA:".length()).trim().toUpperCase();
                    indiceCorreto = letra.charAt(0) - 'A';

                    
                } else if (linha.equals("---")) {
                    if (enunciado != null && indiceCorreto >= 0) {
                        perguntas.add(new Pergunta(enunciado, alternativas, indiceCorreto));
                    }
                    enunciado = null;
                }
            }

            
            
            // Última pergunta sem separador no final
            if (enunciado != null && indiceCorreto >= 0) {
                perguntas.add(new Pergunta(enunciado, alternativas, indiceCorreto));
            }
        }

        
        
        Collections.shuffle(perguntas);
        return perguntas;
    }
}