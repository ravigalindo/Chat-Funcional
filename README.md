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

## Status

Em desenvolvimento, com o canal seguro entre cliente e servidor implementado.

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

## Próxima etapa

A persistência local da chave do histórico ainda precisa ser definida para que o histórico sobreviva ao encerramento do cliente. Também falta concluir a interface de estados do handshake, para informar ao usuário quando um contato ainda não pode receber mensagens.

A renovação de sessões após 60 minutos ou 100 mensagens também requer um protocolo específico de rehandshake no canal já protegido; por isso, não é simulada por uma simples desconexão.