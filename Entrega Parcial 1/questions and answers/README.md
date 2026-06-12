# Questions & Answers — Quiz Multiplayer em Rede

## Entrega Parcial 1

### O que foi implementado nesta entrega

Esta primeira entrega estabelece a **base estrutural** do projeto:

#### Pacote `quiz.modelo`
- **`Pergunta.java`** — Representa uma pergunta com enunciado, 4 alternativas e índice da resposta correta. Implementa `Serializable` para trafegar pela rede.
- **`Partida.java`** — Controla o estado da partida: pontuação dos jogadores, rodada atual e condição de fim de jogo.
- **`Mensagem.java`** — Protocolo de comunicação entre Servidor e Cliente. Define o enum `Tipo` com todos os eventos do jogo (IDENTIFICACAO, NOVA_PERGUNTA, RESULTADO_RODADA, etc.).

#### Pacote `quiz.util`
- **`LeitorPerguntas.java`** — Leitura e parsing do arquivo `recursos/perguntas.txt`. Suporta comentários com `#`, separador `---` e embaralha as perguntas ao carregar.

#### Pacote `quiz.rede`
- **`Servidor.java`** — Abre o `ServerSocket` na porta 12345, aceita 2 clientes, configura os canais de I/O (`ObjectOutputStream` / `ObjectInputStream`) e envia a identificação de cada jogador.

#### Arquivo de perguntas
- **`recursos/perguntas.txt`** — 10 perguntas de cultura geral e programação no formato definido.

---

### Formato do arquivo de perguntas

```
PERGUNTA: Qual é a capital do Brasil?
A: São Paulo
B: Rio de Janeiro
C: Brasília
D: Salvador
RESPOSTA: C
---
```

---

### Tecnologias e conceitos aplicados

| Conceito | Onde aplicado |
|---|---|
| Encapsulamento | Atributos `private` com getters em todas as classes modelo |
| Serializable | `Pergunta`, `Partida`, `Mensagem` para tráfego via `ObjectStream` |
| Threads e Rede | `ServerSocket`, `Socket`, `ObjectOutputStream/InputStream` |
| Leitura de Arquivo | `BufferedReader` + `FileReader` em `LeitorPerguntas` |

---

### Como executar (Parcial 1)

1. Abrir o projeto no NetBeans
2. Executar `Servidor.java` como classe principal
3. O servidor ficará aguardando 2 conexões na porta **12345**

> O cliente GUI e a lógica completa de rodadas serão implementados nas entregas seguintes.

---

### Próximas entregas

- **Parcial 2** — Lógica completa do servidor: rodadas, buzzer com timer de 15s, placar, segunda chance e reinício
- **Parcial 3** — Cliente com interface gráfica Java Swing
- **Final** — Projeto integrado, polido e documentado
