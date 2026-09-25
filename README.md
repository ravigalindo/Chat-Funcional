# Chat - Segurança da Informação

Projeto desenvolvido para a disciplina de Segurança da Informação.

## Descrição

Aplicação de chat desenvolvida em Java, composta por um cliente desktop e um servidor.

## Tecnologias

- Java
- IntelliJ IDEA
- Git
- GitHub

## Estrutura

- `ChatCliente` — aplicação do cliente
- `ChatServidor` — aplicação do servidor
- `lib` — dependências externas, incluindo SQLite e Password4j

## Arquitetura

### Cliente

- Apresentação: `ChatClienteApp`, `LoginView` e `CadastroView`.
- Serviço: sessão do usuário, conversas, presença e coordenação do E2EE.
- Rede e protocolo: `ClienteTCP`, thread de recepção e pacotes protegidos.
- Segurança: classes em `ChatCliente/Seguranca`, sessões do servidor e sessões por contato.
- Persistência: `HistoricoLocalCriptografado`, que grava o histórico local com AES-GCM.

### Servidor

- Serviço: `ClienteHandler` e `GerenciadorClientes`, responsáveis por autenticação, presença, roteamento e fila offline.
- Rede e protocolo: sockets TCP e uma thread por cliente.
- Persistência: `ConexaoSQLite`, `GerenciadorUsuariosBanco` e banco SQLite.
- Segurança: classes em `ChatServidor/Segurança`, responsáveis pelas primitivas e pela sessão do canal.

## Execução

1. Inicie `ChatServidor.ChatServidor`.
2. Inicie `ChatCliente.ChatClienteApp` em duas instâncias para testar uma conversa.
3. Use a mesma porta TCP `5000` e mantenha a pasta `lib` no classpath.

## Status

Em desenvolvimento, com o chat funcional e as camadas de segurança do Projeto 2 em implementação.

## Segurança implementada nesta etapa

- Handshake inicial com Diffie-Hellman efêmero.
- Derivação de 64 bytes com HKDF-SHA-256: 32 bytes para AES-256 e 32 bytes para HMAC-SHA-256.
- Mensagens do canal protegidas por AES-GCM e HMAC-SHA-256.
- Senhas armazenadas com Argon2 e salt individual.
- Cadastro e distribuição inicial de chaves públicas Ed25519.
- Login de dispositivo conhecido por desafio e assinatura digital.
- Login de novo dispositivo com senha, troca da chave pública e descarte das mensagens offline.
- Sessões E2EE independentes por contato, com DH efêmero e HKDF.
- Autenticação mútua dos contatos com desafios assinados por Ed25519.
- Mensagens e histórico trafegados como payload interno cifrado, mantendo a camada do canal externo.
- Mensagens offline armazenadas pelo servidor sem acesso ao conteúdo interno.
- Invalidação das sessões quando um contato troca a chave pública ao entrar em novo dispositivo.

## Pendências conhecidas

- Executar testes de integração com duas instâncias do cliente, incluindo rehandshake e mensagens offline.
- Melhorar a indicação visual de falhas de handshake e de contato sem sessão E2EE.
- Revisar o fluxo GitHub de issues, commits com `Ref #<id>` e merge na `main` conforme o PDF.