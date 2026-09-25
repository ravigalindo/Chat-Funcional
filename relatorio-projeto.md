# Relatorio do projeto Chat

## Progresso atual

Estimativa: 85% dos requisitos dos Projetos 1 e 2 foram implementados.

A implementacao esta avancada, mas ainda falta executar os testes de integracao com servidor e duas instancias do cliente.

## Trabalho realizado

1. Revisao do codigo existente e comparacao com os PDFs dos Projetos 1 e 2.
2. Organizacao do fluxo de seguranca entre cliente e servidor.
3. Implementacao de Diffie-Hellman efemero.
4. Derivacao de chaves com HKDF-SHA-256.
5. Protecao do canal com AES-256 e HMAC-SHA-256.
6. Armazenamento de senhas com Argon2.
7. Cadastro e autenticacao com chaves Ed25519.
8. Login de novo dispositivo com troca da chave publica.
9. Descarte das mensagens offline no login de novo dispositivo.
10. Implementacao de sessoes E2EE independentes por contato.
11. Handshake E2EE com DH e HKDF.
12. Autenticacao mutua entre contatos com desafios assinados.
13. Mensagens protegidas por duas camadas de criptografia.
14. Mensagens offline armazenadas sem texto legivel pelo servidor.
15. Renovacao da sessao segura apos limite de tempo ou mensagens.
16. Historico local cifrado com AES-GCM.
17. Persistencia local da chave privada Ed25519 cifrada com a senha.
18. Correcao do indicador de digitacao e timeout de dois segundos.
19. Remocao do cliente de terminal legado inseguro.
20. Atualizacao da arquitetura e das instrucoes no README.

## Situacao dos clientes antigos

Clientes cadastrados antes da persistencia da chave privada nao possuem uma copia recuperavel da chave. Eles precisam usar uma vez a opcao de novo dispositivo para gerar e salvar uma nova chave local.

## Pendencias

- Executar testes reais com o servidor e dois clientes.
- Testar mensagens online e offline.
- Testar renovacao de sessao.
- Testar carregamento do historico apos reiniciar o cliente.
- Revisar issues, labels, commits com Ref e merge na main.

## Proximo passo

Executar o servidor, abrir dois clientes, cadastrar ou migrar os usuarios, testar o handshake E2EE, enviar mensagens, desconectar um cliente, testar a fila offline e reiniciar o cliente para confirmar o historico cifrado.
