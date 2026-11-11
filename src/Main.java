
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress(8080), 0);

        servidor.createContext("/", Main::processarPedido);
        servidor.start();

        System.out.println("Servidor iniciado!");
        System.out.println("http://localhost:8080");
    }

    private static void processarPedido(HttpExchange exchange)
            throws IOException {

        String caminho = exchange.getRequestURI().getPath();

        if (caminho.equals("/style.css")) {
            String css = Files.readString(Path.of("style.css"));
            enviar(exchange, css, "text/css; charset=UTF-8");
            return;
        }

        if (caminho.equals("/") &&
                exchange.getRequestMethod().equals("GET")) {

            String html = Files.readString(Path.of("index.html"));
            enviar(exchange, html, "text/html; charset=UTF-8");
            return;
        }

        if (caminho.equals("/pedidos") &&
                exchange.getRequestMethod().equals("POST")) {

            String dados = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            Map<String, String> campos = new HashMap<>();

            for (String campo : dados.split("&")) {
                String[] partes = campo.split("=", 2);

                if (partes.length == 2) {
                    campos.put(
                            URLDecoder.decode(partes[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(partes[1], StandardCharsets.UTF_8)
                    );
                }
            }

            String sala = campos.getOrDefault("sala", "").trim();
            String tipo = campos.getOrDefault("tipo", "").trim();
            String descricao = campos.getOrDefault("descricao", "").trim();

            String erro = "";

            if (sala.isEmpty()) {
                erro += "A sala é obrigatória.<br>";
            }

            if (!List.of("Equipamento", "Software", "Rede").contains(tipo)) {
                erro += "Seleciona um tipo de avaria válido.<br>";
            }

            if (descricao.length() < 10) {
                erro += "A descrição deve ter pelo menos 10 caracteres.";
            }

            String resultado;

            if (!erro.isEmpty()) {

                resultado = """
                    <h1>Corrige os seguintes erros</h1>
                    <p class="erro">%s</p>
                    <a href="/">Voltar ao formulário</a>
                    """.formatted(erro);

            } else {

                resultado = """
                    <h1>Pedido validado com sucesso!</h1>
                    <p><strong>Sala:</strong> %s</p>
                    <p><strong>Tipo:</strong> %s</p>
                    <p><strong>Descrição:</strong> %s</p>
                    <a href="/">Voltar ao início</a>
                    """.formatted(
                        escapar(sala),
                        escapar(tipo),
                        escapar(descricao)
                );
            }

            String pagina = """
                <!DOCTYPE html>
                <html lang="pt">
                <head>
                    <meta charset="UTF-8">
                    <link rel="stylesheet" href="/style.css">
                    <title>Resultado do pedido</title>
                </head>
                <body>
                    <div class="container">
                        <div class="formulario">
                            %s
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(resultado);

            enviar(exchange, pagina, "text/html; charset=UTF-8");
            return;
        }

        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void enviar(
            HttpExchange exchange,
            String conteudo,
            String tipo) throws IOException {

        byte[] resposta = conteudo.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", tipo);
        exchange.sendResponseHeaders(200, resposta.length);

        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(resposta);
        }
    }
}
