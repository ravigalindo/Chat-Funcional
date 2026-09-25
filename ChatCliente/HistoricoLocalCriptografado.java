package ChatCliente;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoricoLocalCriptografado {

    private static final Path DIRETORIO =
            Path.of("historico");

    private static final int TAMANHO_SALT = 16;

    private static final int TAMANHO_IV = 12;

    private static final int ITERACOES = 120000;

    private final Path arquivo;

    private final SecretKey chave;

        private final byte[] salt;

    public HistoricoLocalCriptografado(
            String usuario,
            String senha
    ) {

        try {
            Files.createDirectories(DIRETORIO);

            arquivo = DIRETORIO.resolve(
                    nomeArquivo(usuario)
            );

            salt = obterOuCriarSalt();
            chave = derivarChave(senha, salt);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível preparar o histórico local.",
                    e
            );
        }
    }

    public synchronized Map<String, List<Mensagem>> carregar() {

        Map<String, List<Mensagem>> historico =
                new HashMap<>();

        if (!Files.exists(arquivo)) {
            return historico;
        }

        try {
            String pacote =
                    Files.readString(
                            arquivo,
                            StandardCharsets.UTF_8
                    );

            String texto = descriptografar(pacote);

            for (String linha : texto.split("\\n")) {

                if (linha.isBlank()) {
                    continue;
                }

                String[] campos = linha.split("\\t", 5);

                if (campos.length < 5) {
                    continue;
                }

                String contato = decodificar(campos[0]);
                int id = Integer.parseInt(campos[1]);
                String remetente = decodificar(campos[2]);
                String conteudo = decodificar(campos[3]);
                boolean enviadaPorMim = Boolean.parseBoolean(campos[4]);

                historico
                        .computeIfAbsent(
                                contato,
                                chave -> new ArrayList<>()
                        )
                        .add(
                                new Mensagem(
                                        id,
                                        remetente,
                                        conteudo,
                                        enviadaPorMim
                                )
                        );
            }

            return historico;

        } catch (Exception e) {
            System.out.println(
                    "Histórico local não pôde ser carregado: "
                            + e.getMessage()
            );
            return new HashMap<>();
        }
    }

    public synchronized void salvar(
            Map<String, List<Mensagem>> historico
    ) {

        StringBuilder texto =
                new StringBuilder();

        for (Map.Entry<String, List<Mensagem>> entrada :
                historico.entrySet()) {

            for (Mensagem mensagem : entrada.getValue()) {

                texto.append(
                        codificar(entrada.getKey())
                ).append("\t");
                texto.append(mensagem.getId()).append("\t");
                texto.append(codificar(mensagem.getRemetente())).append("\t");
                texto.append(codificar(mensagem.getConteudo())).append("\t");
                texto.append(mensagem.isEnviadaPorMim()).append("\n");
            }
        }

        try {
            Files.writeString(
                    arquivo,
                    criptografar(texto.toString()),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            System.out.println(
                    "Histórico local não pôde ser salvo: "
                            + e.getMessage()
            );
        }
    }

    private byte[] obterOuCriarSalt() throws IOException {

        if (Files.exists(arquivo)) {

            String primeiraLinha =
                    Files.readAllLines(
                            arquivo,
                            StandardCharsets.UTF_8
                    )
                            .stream()
                            .findFirst()
                            .orElse("");

            if (primeiraLinha.startsWith("HIST2|")) {
                String[] partes = primeiraLinha.split("\\|", 3);
                return Base64.getDecoder().decode(
                        partes[1]
                );
            }
        }

        byte[] salt =
                new byte[TAMANHO_SALT];

        new SecureRandom().nextBytes(salt);

        return salt;
    }

    private SecretKey derivarChave(
            String senha,
            byte[] salt
    ) {

        try {
            PBEKeySpec especificacao =
                    new PBEKeySpec(
                            senha.toCharArray(),
                            salt,
                            ITERACOES,
                            256
                    );

            byte[] bytes =
                    SecretKeyFactory
                            .getInstance(
                                    "PBKDF2WithHmacSHA256"
                            )
                            .generateSecret(
                                    especificacao
                            )
                            .getEncoded();

            return new SecretKeySpec(bytes, "AES");

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Não foi possível derivar a chave do histórico.",
                    e
            );
        }
    }

    private String criptografar(String texto) {

        try {
            byte[] iv = new byte[TAMANHO_IV];
            new SecureRandom().nextBytes(iv);

            Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
            cifra.init(
                    Cipher.ENCRYPT_MODE,
                    chave,
                    new GCMParameterSpec(128, iv)
            );

            byte[] cifrado = cifra.doFinal(
                    texto.getBytes(StandardCharsets.UTF_8)
            );

            return "HIST2|"
                    + Base64.getEncoder().encodeToString(salt)
                    + "|"
                    + Base64.getEncoder().encodeToString(iv)
                    + "|"
                    + Base64.getEncoder().encodeToString(cifrado);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Não foi possível cifrar o histórico.",
                    e
            );
        }
    }

    private String descriptografar(String pacote) {

        try {
                        String[] partes = pacote.split("\\|", 4);

                        if (partes.length < 4 || !"HIST2".equals(partes[0])) {
                throw new IllegalArgumentException("Formato inválido");
            }

                        byte[] iv = Base64.getDecoder().decode(partes[2]);
                        byte[] cifrado = Base64.getDecoder().decode(partes[3]);

            Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
            cifra.init(
                    Cipher.DECRYPT_MODE,
                    chave,
                    new GCMParameterSpec(128, iv)
            );

            return new String(
                    cifra.doFinal(cifrado),
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Não foi possível decifrar o histórico.",
                    e
            );
        }
    }

    private static String nomeArquivo(String usuario) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        usuario.getBytes(StandardCharsets.UTF_8)
                )
                + ".dat";
    }

    private static String codificar(String texto) {
        return Base64.getEncoder().encodeToString(
                texto.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String decodificar(String texto) {
        return new String(
                Base64.getDecoder().decode(texto),
                StandardCharsets.UTF_8
        );
    }
}
