package quiz.modelo;

import java.io.Serializable;

/**
 * Protocolo de comunicação entre Servidor e Cliente.
 * Toda mensagem trocada na rede é um objeto desta classe.
 */
public class Mensagem implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Tipo {
        // Servidor → Cliente
        AGUARDANDO_JOGADOR,   // Esperando o 2º jogador conectar / nova partida prestes a começar
        IDENTIFICACAO,        // Informa ao cliente seu número (0 ou 1)
        NOVA_PERGUNTA,        // Envia a pergunta + libera leitura (botão bloqueado)
        LIBERAR_BUZZER,       // 15s passaram, botão vermelho liberado
        BUZZER_BLOQUEADO,     // Outro jogador clicou primeiro
        VOCE_RESPONDEU,       // Confirmação: você pode responder agora
        RESULTADO_RODADA,     // Correto/Errado + placar atualizado
        SEGUNDA_CHANCE,       // Jogador que errou: adversário pode tentar
        FIM_DE_JOGO,          // Partida encerrada
        AGUARDANDO_REINICIO,  // Aguardando ambos confirmarem reinício

        // Cliente → Servidor
        CLICOU_BUZZER,        // Jogador apertou o botão vermelho
        RESPOSTA,             // Jogador enviou alternativa (0-3)
        REINICIAR,            // Solicita nova partida
        CONECTADO             // Handshake inicial
    }

    private final Tipo tipo;
    private Object dado;

    public Mensagem(Tipo tipo) {
        this.tipo = tipo;
    }

    public Mensagem(Tipo tipo, Object dado) {
        this.tipo = tipo;
        this.dado = dado;
    }

    public Tipo getTipo() { return tipo; }
    public Object getDado() { return dado; }

    @Override
    public String toString() {
        return "Mensagem{tipo=" + tipo + ", dado=" + dado + "}";
    }
}
