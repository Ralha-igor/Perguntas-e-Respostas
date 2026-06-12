package quiz.rede;

import quiz.modelo.Mensagem;

import java.io.*;
import java.net.*;

/**
 * Cliente do Quiz Multiplayer - Parcial 1.
 * Conecta ao servidor, recebe identificação e aguarda início.
 */
public class Cliente {

    private static final String HOST  = "127.0.0.1";
    private static final int    PORTA = 12345;

    private ObjectOutputStream output;
    private ObjectInputStream  input;
    private int numeroJogador;

    public static void main(String[] args) throws Exception {
        new Cliente().iniciar();
    }

    public void iniciar() throws Exception {
        System.out.println("=== CLIENTE QUIZ - PARCIAL 1 ===");
        System.out.println("Conectando em " + HOST + ":" + PORTA + " ...");

        Socket socket = new Socket(HOST, PORTA);
        System.out.println("Conectado!");

        //   importante: ObjectOutputStream ANTES de ObjectInputStream
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input  = new ObjectInputStream(socket.getInputStream());

        // aqui ele esta ouvindo mensagens do servidor
        ouvirServidor();
    }

    private void ouvirServidor() throws Exception {
        while (true) {
            Mensagem msg = (Mensagem) input.readObject();
            System.out.println("[RECEBIDO] " + msg);

            switch (msg.getTipo()) {

                case IDENTIFICACAO:
                    numeroJogador = (int) msg.getDado();
                    System.out.println("Você é o Jogador " + (numeroJogador + 1));
                    System.out.println("Aguardando o outro jogador conectar...");
                    break;

                case AGUARDANDO_JOGADOR:
                    System.out.println("[SERVIDOR] " + msg.getDado());
                    System.out.println("Lógica de jogo vem na Parcial 2.");
                    return; // encerra por ora

                default:
                    System.out.println("Mensagem não tratada: " + msg.getTipo());
            }
        }
    }
}