package com.escaperoom.ia.model;

final class DiagnosticoSpringAi {
    private DiagnosticoSpringAi() {}

    static String explicar(Idioma idioma, boolean extenso) {
        return extenso ? idioma.texto("""
                I. RAG y VectorStore
                En el altar conservaste un pasaje completo y descartaste el ruido con un umbral. En Spring Boot, fragmenta documentos, calcula embeddings y consulta un VectorStore con topK y similarityThreshold. Un QuestionAnswerAdvisor puede aportar los fragmentos al modelo. Ajusta tamaño, solapamiento y umbral con documentos de prueba.
                II. MessageWindowChatMemory
                El canto en piedra representa la directiva System. Los ecos recientes son mensajes User y los del guardián son mensajes Assistant. Los suspiros del fuelle representan el presupuesto de tokens, que incluye la respuesta. En Spring Boot, usa MessageChatMemoryAdvisor y MessageWindowChatMemory con un identificador por conversación. Esa ventana cuenta mensajes, aunque el presupuesto del modelo se mida en tokens.
                III. Tool Calling y BeanOutputConverter
                En la compuerta elegiste un mecanismo registrado con argumentos admitidos y un veredicto tipado. En Spring Boot, publica métodos con @Tool, comprueba permisos y argumentos en Java y mapea el JSON con BeanOutputConverter. Valida los campos y contrástalos con el resultado real de la operación.
                IV. ChatClient.stream()
                La cripta transmite la respuesta conforme se genera. En Spring Boot, .stream().content() entrega un Flux<String>. Conecta cada fragmento a SSE y actualiza el texto desde Fetch. Emite el estado verificado como un evento independiente.
                """, """
                I. RAG and VectorStore
                At the altar you kept a whole passage and filtered noise with a threshold. In Spring Boot, split documents, calculate embeddings and query a VectorStore with topK and similarityThreshold. A QuestionAnswerAdvisor can pass retrieved text to the model. Tune size, overlap and threshold with sample documents.
                II. MessageWindowChatMemory
                The stone song represents the System instruction. Recent invocations are User messages and the guardian's echoes are Assistant messages. The bellows' breaths represent the token budget, which includes the reply. In Spring Boot, scope MessageChatMemoryAdvisor and MessageWindowChatMemory by conversation ID. The memory window counts messages even though the model measures its budget in tokens.
                III. Tool Calling and BeanOutputConverter
                At the gate you chose a registered mechanism with valid arguments and a typed verdict. In Spring Boot, expose methods with @Tool, check permissions and arguments in Java and map JSON with BeanOutputConverter. Validate fields against the real operation result.
                IV. ChatClient.stream()
                The crypt streams the reply as it is generated. In Spring Boot, .stream().content() produces a Flux<String>. Send fragments as SSE and append them in Fetch. Emit verified game state in a separate event.
                """).strip() : idioma.texto("""
                RAG y VectorStore: Fragmenta la profecía y recupera pasajes por similitud de significado. En Spring Boot, usa embeddings y un umbral para filtrar ruido.
                MessageWindowChatMemory: Conserva mensajes recientes por conversación y protege la directiva. Presupuesta los tokens de entrada y respuesta.
                @Tool y BeanOutputConverter: Ejecuta una acción Java registrada, valida sus argumentos y comprueba un veredicto tipado.
                ChatClient.stream(): Emite fragmentos con Flux<String> y SSE para pintar el chat mientras el modelo responde.
                """, """
                RAG and VectorStore: Split the prophecy and retrieve passages by semantic similarity. Use embeddings and a threshold to filter noise.
                MessageWindowChatMemory: Keep recent messages per conversation and protect the rule. Budget input and output tokens.
                @Tool and BeanOutputConverter: Run a registered Java action, validate arguments and verify a typed verdict.
                ChatClient.stream(): Send Flux<String> fragments over SSE to update the chat as the model answers.
                """).strip();
    }
}
