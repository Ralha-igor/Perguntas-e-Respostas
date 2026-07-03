package quiz.util;

import quiz.modelo.Partida;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Responsável pela escrita e leitura do histórico de partidas em arquivo texto.
 * Atende ao requisito: "Leitura e Escrita de Arquivos Texto".
 */
public class HistoricoPartidas {

    private static final String CAMINHO = "recursos/historico.txt";
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Registra o resultado de uma partida no arquivo de histórico.
     * Utiliza FileWriter com append=true para acumular partidas sem sobrescrever.
     */
    public static void registrar(Partida partida) {
        try (FileWriter fw = new FileWriter(CAMINHO, true);
             BufferedWriter bw = new BufferedWriter(fw)) {

            String dataHora  = LocalDateTime.now().format(FORMATO);
            int    pontosJ1  = partida.getPontos(0);
            int    pontosJ2  = partida.getPontos(1);
            int    vencedor  = partida.vencedor();
            String nomeJ1    = partida.getNome(0);
            String nomeJ2    = partida.getNome(1);

            String resultado = vencedor == -1
                    ? "EMPATE"
                    : "VENCEDOR: " + (vencedor == 0 ? nomeJ1 : nomeJ2);

            bw.write("========================================");
            bw.newLine();
            bw.write("Data/Hora : " + dataHora);
            bw.newLine();
            bw.write("Rodadas   : " + partida.getTotalRodadas());
            bw.newLine();
            bw.write(nomeJ1 + "  : " + pontosJ1 + " ponto(s)");
            bw.newLine();
            bw.write(nomeJ2 + "  : " + pontosJ2 + " ponto(s)");
            bw.newLine();
            bw.write("Resultado : " + resultado);
            bw.newLine();

            System.out.println("Histórico salvo em: " + CAMINHO);

        } catch (IOException e) {
            System.err.println("Erro ao salvar histórico: " + e.getMessage());
        }
    }

    /** Lê e exibe no console todo o histórico de partidas registradas. */
    public static void exibir() {
        File arquivo = new File(CAMINHO);
        if (!arquivo.exists()) {
            System.out.println("Nenhum histórico encontrado.");
            return;
        }

        System.out.println("\n=== HISTÓRICO DE PARTIDAS ===");
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                System.out.println(linha);
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler histórico: " + e.getMessage());
        }
    }
}
