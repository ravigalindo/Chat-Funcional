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

## Próxima etapa

A criptografia ponta a ponta entre contatos ainda não foi implementada. Ela será desenvolvida em uma etapa própria, incluindo handshake DH por contato, autenticação mútua, duas camadas de criptografia, mensagens offline cifradas e histórico local protegido.

A renovação de sessões após 60 minutos ou 100 mensagens também requer um protocolo específico de rehandshake no canal já protegido; por isso, não é simulada por uma simples desconexão.