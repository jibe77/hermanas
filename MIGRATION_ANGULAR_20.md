# Migration Angular 18 → 20

Plan de mise à niveau préparé le 2026-05-27. À exécuter en plusieurs sessions.
Aucune ligne de code n'a été modifiée pour produire ce document ; il sert de feuille de route.

---

## Pourquoi ce document ?

Angular impose de migrer **un seul majeur à la fois** (`ng update` refuse de sauter de 18
directement à 20). Il faut donc passer par 19. Plutôt que de naviguer en aveugle pendant
la migration, on inventorie d'abord tout ce qui doit bouger et on valide les versions cibles
de chaque dépendance tierce.

---

## État actuel (snapshot 2026-05-27)

| Composant | Version actuelle | Cible Angular 20 |
|---|---|---|
| `@angular/*` | `18.2.14` | `20.3.21` (dernière 20 LTS) |
| `@angular/cli` | `18.2.21` | `20.3.x` |
| `@angular/cdk` + `@angular/material` | `18.2.x` | `20.x` |
| TypeScript | `5.4.5` | `5.8.x` (Angular 20 exige ≥ 5.8) |
| Node | `20.19.5` (machine de dev) | **OK** — Angular 20 exige `^20.19.0 \|\| ^22.12.0 \|\| ≥24` |
| `rxjs` | `7.8.1` | inchangé (`^7.4.0` supporté) |
| `zone.js` | `0.14.10` | `0.15.x` |
| `@ng-bootstrap/ng-bootstrap` | `13.1.1` (= Angular 13 !) | `19.0.x` (= Angular 20) |
| `@fortawesome/angular-fontawesome` | `0.13.0` | `2.0.x` (= Angular 20) |
| `@fortawesome/fontawesome-svg-core` + free-icons | `6.5.x` | rester à `^6.5` (versions 7+ disponibles mais non requises) |
| `@angular-eslint/*` | `16.3.1` (= Angular 16 !) | `20.x` |
| `eslint` | `7.32.0` | `^9.x` (eslint 7 est EOL ; angular-eslint 20 exige `^8.57 \|\| ^9`) |
| `@stomp/rx-stomp` | `2.0.0` | `2.4.0` (peer deps ok) |
| `@stomp/stompjs` | `7.2.1` | OK |
| `uuid` | `8.3.2` | `^9` (peer de rx-stomp 2.4) |
| `chart.js` | `3.6.2` | au moins maintenu (`4.5.1` disponible si on veut suivre) |
| `karma` + `jasmine` | `6.3.2` / `3.7.1` | encore supportés mais **dépréciation officielle** dans Angular 20 |
| `@sbpro/ng` | `1.4.2` | **à retirer** (voir section dédiée) |
| `prettier` | `2.2.1` | `3.x` (config différente) |

Le décalage le plus criant : **`@ng-bootstrap`** et **`@angular-eslint`** sont restés ancrés
sur leurs versions d'origine (`13` et `16`) alors qu'Angular est passé à 18. Le passage à 20
oblige à les remettre au niveau du runtime — il faut donc s'attendre à des frictions
indépendantes des migrations Angular automatiques.

---

## `@sbpro/ng` — clarification

C'est un paquet de **schematics** (générateurs `ng generate`) issu du template
[Start Bootstrap Pro](https://startbootstrap.com/theme/sb-admin-pro-angular).

- Utilisé pour scaffolder l'application initiale (`generate:component`, `generate:module`,
  `generate:service`, `generate:directive` dans `package.json:17-20`)
- **N'apparaît jamais dans un `import` runtime** du code source — vérification :
  `grep -r "from '@sbpro/ng'" frontend/src` → 0 résultat
- Les `selector: 'sb-*'` du code (ex. `sb-layout-dashboard`, `sb-card`) sont des
  composants **locaux** générés par les schematics, qui vivent maintenant dans `frontend/src/modules/`
  et ne dépendent plus du paquet
- Ses propres deps datent d'Angular 9 (`@angular-devkit/core@9.1.7`) — il ne sera **jamais**
  compatible avec une CLI Angular 20+

**Action** : le supprimer de `devDependencies` à la première étape. Conséquences :
- On perd `npm run generate:*`. Ce n'est pas dramatique — on peut les remplacer par les
  schematics standard d'Angular CLI (`ng generate component`, `ng generate module`, etc.) ou
  par les générateurs Nx si on en arrive là.
- Aucun impact runtime, aucun composant à réécrire.

---

## Plan global (3 sessions distinctes recommandées)

### Session 1 — Préparation (aucun changement de version Angular)

Objectif : nettoyer l'environnement pour que `ng update` ait toutes les chances de réussir.

1. **Commit propre.** La branche `master` a actuellement 40+ fichiers modifiés non commités —
   il faut soit les committer, soit les stash avant la migration.
2. **Retirer `@sbpro/ng`** de `devDependencies` :
   - Supprimer les scripts `generate:*` de `package.json`
   - `npm uninstall @sbpro/ng`
   - Documenter dans `CLAUDE.md` la commande de remplacement (`ng generate component …`)
3. **Vérifier la version Node** : on est à 20.19.5, donc compatible Angular 20 (`^20.19.0`).
   Optionnel mais conseillé : passer à **Node 22 LTS** pour avoir de la marge (et parce que
   beaucoup d'outillage va commencer à exiger 22 d'ici 1 an).
4. **Mettre à jour les deps qui sont en retard et qui le peuvent sans changer Angular** :
   - `prettier` 2.2 → 3.x (changements de config — voir `.prettierrc.json`)
   - `eslint` 7 → 9 (eslint 7 est EOL ; @angular-eslint actuel ne le supporte plus)
   - `@angular-eslint/*` 16 → 18 (= notre Angular actuel)
   - `@stomp/rx-stomp` 2.0 → 2.4, `uuid` 8 → 9
5. **`ng build` + `mvn package`** → tout doit encore passer.
6. **Snapshot et tag** : `git tag pre-ng19-migration`.

À la fin de la session 1, l'app tourne toujours sur Angular 18 mais l'environnement
de build est propre et capable de digérer un `ng update`.

### Session 2 — Angular 18 → 19

```bash
cd frontend
npx ng update @angular/core@19 @angular/cli@19
npx ng update @angular/cdk@19 @angular/material@19
```

Migrations à attendre (certaines automatiques via les schematics d'`ng update`) :

- **`@angular/localize`** : version 19 reste compatible avec le format xlf 1.2 actuel
  (`messages.xlf` + `messages.fr-FR.xlf`) ; pas de migration de fichiers attendue.
- **Standalone components** : Angular 19 active le mode `standalone: true` par défaut.
  Le code actuel utilise des `NgModule` partout — il continuera à fonctionner, mais les
  schematics génèreront du standalone. **Décision à prendre** : on laisse cohabiter, ou
  on migre tout en standalone (gros travail, optionnel, repoussable à plus tard).
- **Control flow `@if/@for/@switch`** : optionnel. Schematic disponible :
  `ng generate @angular/core:control-flow`. À faire après le passage à 19, pas pendant.
- **`provideAppInitializer()`** : remplace `APP_INITIALIZER` (deprecated en 19). Migration
  automatique. À surveiller dans `app.module.ts:50-55` où on bloque le bootstrap sur
  `userService.initialAuthCheck()`.
- **Deps tierces à bumper** :
  - `@ng-bootstrap/ng-bootstrap` `13.1.1` → `18.0.0` (= Angular 19). **C'est un saut de 5
    majeurs** — il faut s'attendre à des API renommées ou retirées. Composants utilisés
    actuellement : `NgbModule` global dans `AppModule`, `NgbDropdownModule` dans le menu
    système, `NgbDatepicker` ailleurs. Lire le [changelog ng-bootstrap](https://ng-bootstrap.github.io/#/changelog) pour les 14→18.
  - `@fortawesome/angular-fontawesome` `0.13.0` → `2.0.0` (= Angular 19). Sauts majeurs
    aussi — l'API `<fa-icon [icon]>` est stable mais les schematics changent.
  - `@angular-eslint/*` 18 → 19
- **TypeScript** : 5.5 ou 5.6 (Angular 19 supporte 5.5+).
- **Tests** : `karma + jasmine` continuent à fonctionner.

`ng build` + `mvn test` → 71/71. Tag : `git tag post-ng19-migration`.

### Session 3 — Angular 19 → 20

```bash
cd frontend
npx ng update @angular/core@20 @angular/cli@20
npx ng update @angular/cdk@20 @angular/material@20
```

- **TypeScript** : passer à 5.8.x (Angular 20 l'exige).
- **`zone.js`** : `~0.14` → `~0.15`.
- **`@ng-bootstrap`** : 18 → 19 (= Angular 20). **NB** : `ng-bootstrap` `20.0.0` (paru
  récemment) cible déjà Angular 21, ne pas s'y tromper.
- **`@fortawesome/angular-fontawesome`** : 2 → 2 ou 3 (la 2.x est compatible Angular 19,
  la 3.x cible Angular 20 — utiliser la 3.x).
- **`@angular-eslint/*`** : 19 → 20.
- **Karma/Jasmine** : encore là, mais Angular 20 marque officiellement la dépréciation et
  pousse vers Vitest ou Web Test Runner. À évaluer en session 4 si on veut investir
  maintenant ou attendre Angular 21/22.
- **Signal-based forms** : nouveauté Angular 20, totalement optionnelle.
- **Effect()** stable depuis 19, déjà utilisable.

`ng build` + `mvn test` → 71/71. Tag : `git tag post-ng20-migration`.

---

## Points d'attention spécifiques à Hermanas

1. **Bundle dans le JAR Spring Boot** (`pom.xml` + `frontend-maven-plugin`). Le build NPM
   tourne pendant `mvn package` — toute régression de la commande `npm run build` casse le
   livrable backend. À chaque session : faire `mvn package` (pas juste `ng build`) avant
   de tagger.

2. **Service Worker** (`ServiceWorkerModule.register('ngsw-worker.js', ...)` dans
   `app.module.ts:28-33`). À chaque major Angular, vérifier la compat de
   `@angular/service-worker` et le `ngsw-config.json` — surtout entre 18 et 19 où la
   stratégie de cache a légèrement changé.

3. **i18n natif** (`messages.fr-FR.xlf` ≈ 950 lignes). Angular n'a **pas** déprécié son i18n
   natif en 20, donc rien à craindre, mais re-runner `ng extract-i18n` après la migration
   pour rafraîchir les `source-file` et `linenumber` des contextes.

4. **`APP_INITIALIZER`** (`app.module.ts:50-55`) bloque le bootstrap sur
   `userService.initialAuthCheck()`. Le passage à `provideAppInitializer()` est mécanique
   mais critique — si ce hook casse, on revoit l'écran de login flasher avant d'aller sur
   `/logs`. Tester cliniquement après la session 2.

5. **`@PreAuthorize` côté backend + signaux côté front** (`UserService.isAdmin()`). La
   logique d'affichage conditionnel utilise déjà des `computed()` (`side-nav.component.ts`).
   Angular 20 ne change rien à l'API des signals — pas de risque ici.

6. **WebSocket STOMP** (`@stomp/rx-stomp`). Le paquet est stable et compatible
   rxjs 7+, mais il faut bump `uuid` 8 → 9 en même temps que rx-stomp 2.4 (peer dep).

7. **Charts.js 3** : si on veut éviter de toucher à `chart.js`, le garder en 3.x.
   La 4.x est dispo mais l'API du module `chart.js` a bougé entre 3 et 4 et nos composants
   de dashboard utilisent l'ancienne API.

8. **`@sbpro/ng`** : à supprimer **avant** la session 2, sinon `ng update` va se plaindre
   d'une peer dep résiduelle.

9. **Build du backend qui dépend du frontend** : pendant les sessions 2 et 3, ne pas
   lancer `mvn package` en parallèle d'un `npm run build` — la sortie va dans
   `target/classes/static/` et les deux processus se marcheraient dessus.

---

## Commandes de check à exécuter après chaque session

```bash
# Frontend
cd frontend
npx ng build --configuration=production
npx ng test --watch=false --browsers=ChromeHeadless
npx eslint .

# Backend (vérifie aussi le bundling du frontend dans le JAR)
cd ..
mvn clean package
mvn test
```

Sortie attendue : 71/71 backend, frontend build OK, JAR ≈ 64 MB self-contained.

---

## Décisions à prendre avant de démarrer la session 1

| Question | Recommandation |
|---|---|
| Passer Node 20 → 22 LTS ? | **Oui** — meilleure marge pour Angular 20+ |
| Migrer les `NgModule` en standalone components ? | **Non pendant la migration** — à reporter |
| Migrer `*ngIf`/`*ngFor` vers `@if`/`@for` ? | **Non pendant la migration** — schematic dispo plus tard |
| Remplacer Karma par Vitest ? | **Non pendant la migration** — Karma encore supporté en 20 |
| Bump `chart.js` 3 → 4 ? | **Non pendant la migration** — sujet indépendant |
| Garder `@sbpro/ng` ? | **Non** — l'enlever en session 1 |
