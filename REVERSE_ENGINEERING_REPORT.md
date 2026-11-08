# REVERSE_ENGINEERING_REPORT.md

**Repositório:** FHIR-CareCommunication-demo-Java  
**URL:** https://github.com/medcomdk/FHIR-CareCommunication-demo-Java  
**Branch base analisada:** main (commit 0a5a136c3751271ed9d0e87905f59af9ed3a5710)  
**Fork alvo:** https://github.com/admeduardovieira/FHIR-CareCommunication-demo-Java  
**Data da auditoria:** 2026-11-08  
**Ambiente:** Windows, PowerShell 5.1, Git 2.49.0, Java 17.0.20 (Maven não instalado)  
**Agente:** Kilo (REA 6.7.0), identidade Git admeduardovieira / admeduardovieira@users.noreply.github.com  

---

## A. Diagnóstico geral

### Arquitetura identificada
FHIR-CareCommunication-demo-Java é um projeto demo em Java/Spring Boot que demonstra o uso das bibliotecas oficiais HL7 FHIR (HAPI FHIR) para parsear e serializar mensagens de Care Communication de acordo com o implementation guide da MedCom. Não pretende ser uma implementação completa, mas uma amostra orientadora.

É um projeto Maven multi-módulo (Spring Boot 2.6.3, Java 11):
- `common` — biblioteca de validação de mensagens Care Communication FHIR (Validator.java)
- `sender` — aplicação console para criar uma mensagem Care Communication FHIR (ResourceFactory, SenderApplication)
- `receiver` — aplicação console para parsear uma mensagem Care Communication FHIR (ResourceParser, ReceiverApplication)
- `src` — aplicação de teste/demo (Demo1Application, Demo1ApplicationTests)

Dependências FHIR: HAPI FHIR 5.6.2 (hapi-fhir-validation, hapi-fhir-structures-r4, hapi-fhir-validation-resources-r4).

Pontos de entrada: `SenderApplication` (cria e serializa), `ReceiverApplication` (parse e imprime), `ResourceFactory` (constrói recursos FHIR), `Validator` (constrói validador com NpmPackageValidationSupport), `ResourceParser` (extrai recursos do bundle).

### Tecnologias e dependências
- Java 11
- Spring Boot 2.6.3 (starter, starter-test)
- HAPI FHIR 5.6.2 (validation, structures-r4, validation-resources-r4)
- JUnit 5 (Spring Boot test)

### Estado de manutenção
Histórico curto. Último commit `0a5a136` ("Added more documentation"). Projeto demo com 3 módulos + módulo de teste. README detalhado documentando parsing, serialização e validação FHIR. Único teste existente é `contextLoads` (vazio).

### Mecanismos de testes existentes
- Um teste `Demo1ApplicationTests.contextLoads()` (vazio, @SpringBootTest).
- Nenhum teste de validação/parsing/serialização das mensagens Care Communication.
- Nenhum CI/CD visível (ausência de `.github/workflows`).

### Limitações da investigação
- Maven não instalado (`mvn` não reconhecido); impossível executar `mvn test` ou `mvn clean install`.
- Java 17 disponível, projeto requer Java 11; não foi possível compilar/executar testes.
- Testes baseados em leitura estática de código .java.
- Não foram acessados issues/PRs do GitHub além do conteúdo do clone.

---

## B. Problemas encontrados

| ID | Título | Severidade | Status | Localização |
|----|--------|------------|--------|-------------|
| REA-012 | NPE ao acessar `.getValue()` de extensão que pode ser null | P1 | CONFIRMADO | `receiver/.../ReceiverApplication.java:90` |
| REA-013 | ClassCastException por cast direto `(Practitioner)` sem instanceof | P1 | CONFIRMADO | `receiver/.../ReceiverApplication.java:94` |
| REA-014 | NoSuchElementException por `.get()` em Optional.empty() | P2 | CONFIRMADO | `receiver/.../ResourceParser.java:16` |

### Detalhes

**REA-012 — NPE em `getExtensionByUrl().getValue()` (CONFIRMADO)**
- Problema: `ReceiverApplication.java:90` chama `payload.getExtensionByUrl(Validator.AUTHOR_EXTENSION_URL).getValue()`. Se a extensão de autor não estiver presente, `getExtensionByUrl` retorna null e `.getValue()` lança NullPointerException.
- Evidência: `ReceiverApplication.java:90` — `var valueReference = payload.getExtensionByUrl(Validator.AUTHOR_EXTENSION_URL).getValue();`
- Causa-raiz: acesso a método em resultado potencialmente null sem verificação.
- Impacto: o receiver pode falhar com NPE ao processar uma mensagem sem a extensão de autor, quebrando a operação essencial de parsing.
- Correção proposta: verificar null da extensão antes de chamar `.getValue()`.

**REA-013 — ClassCastException em cast direto (CONFIRMADO)**
- Problema: `ReceiverApplication.java:94` faz cast direto `(Practitioner) ((Reference) valueReference).getResource()` sem verificar `instanceof`. Se o recurso referenciado não for um Practitioner, lança ClassCastException.
- Evidência: `ReceiverApplication.java:94` — `var practitioner = (Practitioner) ((Reference) valueReference).getResource();`
- Causa-raiz: confiança implícita de que o tipo do recurso é Practitioner, sem validação de tipo em tempo de execução.
- Impacto: o receiver pode falhar com ClassCastException se o tipo do recurso diferir do esperado.
- Correção proposta: verificar `instanceof Practitioner` antes do cast.

**REA-014 — NoSuchElementException em `.get()` (CONFIRMADO)**
- Problema: `ResourceParser.java:16` chama `.get()` no resultado de `singleElement()`, que retorna `Optional.empty()` quando a lista não tem exatamente 1 elemento. `.get()` em Optional.empty() lança NoSuchElementException.
- Evidência: `ResourceParser.java:14-16` — `.collect(singleElement()).get()`
- Causa-raiz: uso de `.get()` sem verificar `isPresent()`.
- Impacto: o parsing falha com exceção se não houver exatamente um recurso de cada tipo no bundle.
- Correção proposta: verificar `isPresent()` ou usar `orElseThrow` com mensagem descritiva.

---

## C. Problemas recorrentes

Nenhum problema recorrente documentado publicamente foi verificado (escopo limitado ao conteúdo do clone). O projeto é um demo com histórico curto e sem issues/PRs acessados nesta auditoria.

---

## D. Erros não documentados

Erros identificados pela auditoria que não constam em documentação do projeto:
- REA-012, REA-013, REA-014 (todos acima).

---

## E. Performance

Nenhuma oportunidade crítica de performance identificada na inspeção estática. O projeto é um demo de console. Limitação: sem Maven, impossível executar benchmarks.

---

## F. Melhorias técnicas

| Área | Melhoria | Justificativa |
|------|----------|---------------|
| Robustez | Adicionar null-check na extensão (REA-012) | Evitar NPE no parsing |
| Robustez | Adicionar `instanceof` antes do cast (REA-013) | Evitar ClassCastException |
| Robustez | Verificar `isPresent()` antes de `.get()` (REA-014) | Evitar NoSuchElementException |
| Testes | Adicionar testes de parsing/validação de mensagens Care Communication | Cobertura de comportamentos essenciais (atualmente só existe contextLoads vazio) |

---

## G. Plano de execução

Correções independentes, baixo risco, comprovadas por código:
1. **REA-012** (P1): `ReceiverApplication.java:90` — null-check na extensão
2. **REA-013** (P1): `ReceiverApplication.java:94` — `instanceof` antes do cast
3. **REA-014** (P2): `ResourceParser.java:16` — `isPresent()` antes de `.get()`

Ordem recomendada: REA-012 → REA-013 → REA-014.

---

## H. Resultado esperado

- Eliminação de 2 riscos de exceção em tempo de execução (NPE, ClassCastException) no receiver.
- Eliminação de 1 risco de NoSuchElementException no parser de recursos.
- Código mais robusto sem alterar a lógica de negócio nem a API pública.
- Contribuições de baixo risco, adequadas a PRs pequenos e revisáveis.

---

## Perguntas finais obrigatórias

1. **Quais bugs críticos foram comprovados?** Nenhum P0. Dois erros P1 (NPE e ClassCastException no receiver) confirmados por inspeção de código.
2. **Quais problemas repetitivos os desenvolvedores ainda não resolveram?** Nenhum verificado publicamente (projeto demo, histórico curto).
3. **Quais erros importantes não estavam documentados?** NPE em extensão ausente, ClassCastException em cast direto, NoSuchElementException em Optional.get().
4. **Quais oportunidades de performance foram encontradas?** Nenhuma crítica identificada.
5. **Quais melhorias de segurança e confiabilidade são justificáveis?** Verificações de null e tipo no receiver (confiabilidade).
6. **Quais correções podem ser feitas rapidamente?** REA-012, REA-013, REA-014 (pequenas, baixo risco, comprovadas por código).
7. **Quais são as três contribuições com maior potencial de valor técnico?** (1) Null-check na extensão (evita NPE no parsing), (2) instanceof antes do cast (evita ClassCastException), (3) isPresent antes de .get() (evita NoSuchElementException).
8. **Quais correções devem ser evitadas por falta de evidência:** Alterar lógica de validação FHIR ou serialização sem execução de testes (sem Maven não é possível validar).
9. **Quais modificações você recomenda implementar primeiro?** REA-012, REA-013, REA-014 (pequenas, baixo risco, comprovadas por código).

---

## Análise de malware e ameaças (estática)

Executada em todos os arquivos do repositório (.java, .xml, .json, .md, .gitignore), excluindo `.git`.

**Resultado: Nenhum indicador identificado pela análise estática realizada.**

- Não há scripts executáveis suspeitos fora do código-fonte Java legítimo.
- Nenhuma chave, credencial, token, senha ou string de conexão embutida.
- Nenhuma ofuscação, empacotamento, persistência, execução de processos, escrita/remoção de arquivos, coleta/exfiltração de dados ou modificação de configuração suspeita.
- O `SenderApplication.java` grava um arquivo em `java.io.tmpdir` (funcionalidade legítima de demo, não suspeita).
- Limitação: análise estática apenas; Maven não instalado para verificar dependências transitivas. Não se afirma que o projeto está livre de malware.

---

**Nota:** Este relatório é a versão base em português. Versões sincronizadas em inglês (`.en.md`) e ucraniano (`.uk.md`) acompanham este documento, preservando IDs REA-*, severidades P0–P3, caminhos, comandos, URLs e trechos de código inalterados.
