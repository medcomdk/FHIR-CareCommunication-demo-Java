# CORRECTIONS.md

**Repositório:** FHIR-CareCommunication-demo-Java  
**URL:** https://github.com/medcomdk/FHIR-CareCommunication-demo-Java  
**Branch base analisada:** main (commit 0a5a136c3751271ed9d0e87905f59af9ed3a5710)  
**Fork alvo:** https://github.com/admeduardovieira/FHIR-CareCommunication-demo-Java  
**Data:** 2026-11-08  
**Agente:** Kilo (REA 6.7.0), identidade Git admeduardovieira / admeduardovieira@users.noreply.github.com  

---

## Correções propostas e status

| ID | Título | Severidade | Status | Localização | Risco | PR sugerido |
|----|--------|------------|--------|-------------|-------|-------------|
| REA-012 | Null-check na extensão antes de .getValue() | P1 | Proposta (não implementada) | `receiver/.../ReceiverApplication.java:90` | Baixo | `fix/rea-012-null-check-extension` |
| REA-013 | `instanceof` antes do cast `(Practitioner)` | P1 | Proposta (não implementada) | `receiver/.../ReceiverApplication.java:94` | Baixo | `fix/rea-013-instanceof-before-cast` |
| REA-014 | `isPresent()` antes de `.get()` no parser | P2 | Proposta (não implementada) | `receiver/.../ResourceParser.java:16` | Baixo | `fix/rea-014-is-present-before-get` |

---

## Detalhes por correção

### REA-012 — Null-check na extensão

**Problema:** `ReceiverApplication.java:90` acessa `.getValue()` em resultado de `getExtensionByUrl()` que pode ser null.

**Evidência:** `ReceiverApplication.java:90` — `var valueReference = payload.getExtensionByUrl(Validator.AUTHOR_EXTENSION_URL).getValue();`

**Causa-raiz:** acesso a método em resultado potencialmente null.

**Correção proposta:** verificar null da extensão; se ausente, pular ou tratar adequadamente.

**Arquivos envolvidos:** `receiver/src/main/java/com/trifork/receiver/ReceiverApplication.java`

**Teste de validação:** executar testes existentes e adicionar teste de parsing de mensagem sem extensão de autor.

**Risco:** baixo.

**Benefício:** evitar NPE no parsing.

**Prioridade:** imediata.

---

### REA-013 — `instanceof` antes do cast

**Problema:** `ReceiverApplication.java:94` faz cast direto `(Practitioner)` sem `instanceof`.

**Evidência:** `ReceiverApplication.java:94` — `var practitioner = (Practitioner) ((Reference) valueReference).getResource();`

**Causa-raiz:** confiança implícita no tipo do recurso.

**Correção proposta:** verificar `valueReference.getResource() instanceof Practitioner` antes do cast.

**Arquivos envolvidos:** `receiver/src/main/java/com/trifork/receiver/ReceiverApplication.java`

**Teste de validação:** executar testes existentes e adicionar teste com recurso de tipo diferente.

**Risco:** baixo.

**Benefício:** evitar ClassCastException.

**Prioridade:** imediata.

---

### REA-014 — `isPresent()` antes de `.get()`

**Problema:** `ResourceParser.java:16` chama `.get()` em Optional que pode estar vazio.

**Evidência:** `ResourceParser.java:14-16` — `.collect(singleElement()).get()`

**Causa-raiz:** uso de `.get()` sem verificar `isPresent()`.

**Correção proposta:** verificar `isPresent()` ou usar `orElseThrow` com mensagem descritiva.

**Arquivos envolvidos:** `receiver/src/main/java/com/trifork/receiver/ResourceParser.java`

**Teste de validação:** executar testes existentes e adicionar teste com bundle sem recurso de determinado tipo.

**Risco:** baixo.

**Benefício:** evitar NoSuchElementException.

**Prioridade:** imediata.

---

## Problemas não corrigidos e riscos remanescentes

- Todas as correções estão em status "Proposta" nesta etapa (Fase 1 concluída). Nenhuma alteração aplicada ao código-fonte ainda.
- Limitação: Maven não instalado; não foi possível executar `mvn test` para validar dinamicamente.
- Riscos remanescentes: os 3 problemas acima ainda presentes no código.

---

## Registros de relatório

- Base PT: `REVERSE_ENGINEERING_REPORT.md` e `CORRECTIONS.md` (este arquivo).
- Traduções: `REVERSE_ENGINEERING_REPORT.en.md`, `REVERSE_ENGINEERING_REPORT.uk.md`, `CORRECTIONS.en.md`, `CORRECTIONS.uk.md`.
- Todos na raiz do clone de trabalho / área de auditoria, ao lado dos relatórios-base em português.

**Nota de tradução (uk):** Tradução técnica assistida automaticamente; recomenda-se revisão por falante nativo antes de uso como referência normativa. Não houve revisão por nativo obtida.
