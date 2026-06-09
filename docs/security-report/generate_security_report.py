#!/usr/bin/env python3
"""
Generate the Hermanas security report as a PDF using ReportLab.

Output: docs/security-report/hermanas-security-report.pdf

The content is derived from a fresh code audit (June 2026) of the master branch,
focused on Spring Boot 2.7.18 / Java 11 / Pi Zero backend + Angular 20 SPA, served
publicly at www.hermanas.fr behind a Caddy reverse proxy.
"""

from datetime import date
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm, mm
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    KeepTogether,
    NextPageTemplate,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


# ─── Palette ──────────────────────────────────────────────────────────────────

NAVY = colors.HexColor("#1F4E78")
LIGHT_BLUE = colors.HexColor("#D9E1F2")
MED_GREY = colors.HexColor("#808080")
DARK_GREY = colors.HexColor("#333333")
LIGHT_GREY = colors.HexColor("#F2F2F2")
RED = colors.HexColor("#C00000")
ORANGE = colors.HexColor("#ED7D31")
GREEN = colors.HexColor("#548235")
YELLOW = colors.HexColor("#BF8F00")


# ─── Styles ───────────────────────────────────────────────────────────────────

styles = getSampleStyleSheet()

TITLE = ParagraphStyle(
    "TitleStyle",
    parent=styles["Title"],
    fontName="Helvetica-Bold",
    fontSize=26,
    textColor=NAVY,
    spaceAfter=14,
    leading=30,
)

SUBTITLE = ParagraphStyle(
    "Subtitle",
    parent=styles["Normal"],
    fontName="Helvetica",
    fontSize=14,
    textColor=DARK_GREY,
    alignment=TA_CENTER,
    spaceAfter=8,
    leading=18,
)

META = ParagraphStyle(
    "Meta",
    parent=styles["Normal"],
    fontName="Helvetica",
    fontSize=10,
    textColor=MED_GREY,
    alignment=TA_CENTER,
    spaceAfter=4,
    leading=14,
)

H1 = ParagraphStyle(
    "H1",
    parent=styles["Heading1"],
    fontName="Helvetica-Bold",
    fontSize=18,
    textColor=NAVY,
    spaceBefore=20,
    spaceAfter=10,
    leading=22,
)

H2 = ParagraphStyle(
    "H2",
    parent=styles["Heading2"],
    fontName="Helvetica-Bold",
    fontSize=14,
    textColor=NAVY,
    spaceBefore=14,
    spaceAfter=6,
    leading=18,
)

H3 = ParagraphStyle(
    "H3",
    parent=styles["Heading3"],
    fontName="Helvetica-Bold",
    fontSize=11,
    textColor=DARK_GREY,
    spaceBefore=8,
    spaceAfter=4,
    leading=14,
)

BODY = ParagraphStyle(
    "Body",
    parent=styles["Normal"],
    fontName="Helvetica",
    fontSize=10,
    textColor=DARK_GREY,
    leading=14,
    alignment=TA_JUSTIFY,
    spaceAfter=6,
)

BULLET = ParagraphStyle(
    "Bullet",
    parent=BODY,
    leftIndent=16,
    bulletIndent=4,
    spaceAfter=3,
)

CODE = ParagraphStyle(
    "Code",
    parent=BODY,
    fontName="Courier",
    fontSize=9,
    textColor=DARK_GREY,
    backColor=LIGHT_GREY,
    leftIndent=8,
    rightIndent=8,
    spaceBefore=3,
    spaceAfter=6,
    leading=12,
)

SMALL = ParagraphStyle(
    "Small",
    parent=BODY,
    fontSize=9,
    textColor=MED_GREY,
)

BADGE_STYLES = {
    "Critical": ("Critique", RED, colors.white),
    "High": ("Élevé", ORANGE, colors.white),
    "Medium": ("Moyen", YELLOW, colors.white),
    "Low": ("Faible", GREEN, colors.white),
    "Info": ("Info", NAVY, colors.white),
}


def severity_para(level: str) -> Paragraph:
    label, bg, fg = BADGE_STYLES[level]
    return Paragraph(
        f'<para><font color="{fg.hexval()}" backColor="{bg.hexval()}">'
        f"&nbsp;&nbsp;{label}&nbsp;&nbsp;</font></para>",
        BODY,
    )


# ─── Page template (header + footer) ─────────────────────────────────────────

REPORT_TITLE = "Rapport de sécurité — Hermanas"
REPORT_DATE = date.today().strftime("%d %B %Y")


def draw_header_footer(canvas, doc):
    canvas.saveState()
    width, height = A4
    # Header strip
    canvas.setFillColor(NAVY)
    canvas.rect(0, height - 18 * mm, width, 8 * mm, stroke=0, fill=1)
    canvas.setFont("Helvetica-Bold", 9)
    canvas.setFillColor(colors.white)
    canvas.drawString(15 * mm, height - 14 * mm, REPORT_TITLE)
    canvas.drawRightString(width - 15 * mm, height - 14 * mm, REPORT_DATE)
    # Footer
    canvas.setFillColor(MED_GREY)
    canvas.setFont("Helvetica", 8)
    canvas.drawString(
        15 * mm,
        10 * mm,
        "Confidentiel — diffusion interne. Hermanas v0.8.1.",
    )
    canvas.drawRightString(width - 15 * mm, 10 * mm, f"Page {doc.page}")
    canvas.restoreState()


def draw_cover(canvas, doc):
    canvas.saveState()
    width, height = A4
    canvas.setFillColor(NAVY)
    canvas.rect(0, 0, width, height, stroke=0, fill=1)
    # Decorative band
    canvas.setFillColor(colors.white)
    canvas.rect(0, height / 2 - 60 * mm, width, 120 * mm, stroke=0, fill=1)
    canvas.restoreState()


def build_doc(filename: Path) -> BaseDocTemplate:
    doc = BaseDocTemplate(
        str(filename),
        pagesize=A4,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=22 * mm,
        bottomMargin=18 * mm,
        title=REPORT_TITLE,
        author="Hermanas — équipe technique",
        subject="Security review",
    )
    frame = Frame(
        doc.leftMargin,
        doc.bottomMargin,
        doc.width,
        doc.height,
        id="body",
        showBoundary=0,
    )
    cover_frame = Frame(
        0,
        0,
        doc.pagesize[0],
        doc.pagesize[1],
        leftPadding=30 * mm,
        rightPadding=30 * mm,
        topPadding=60 * mm,
        bottomPadding=40 * mm,
        id="cover",
    )
    doc.addPageTemplates(
        [
            PageTemplate(id="cover", frames=[cover_frame], onPage=draw_cover),
            PageTemplate(id="body", frames=[frame], onPage=draw_header_footer),
        ]
    )
    return doc


# ─── Content helpers ──────────────────────────────────────────────────────────

def h1(text):
    return Paragraph(text, H1)


def h2(text):
    return Paragraph(text, H2)


def h3(text):
    return Paragraph(text, H3)


def p(text):
    return Paragraph(text, BODY)


def li(text):
    return Paragraph(text, BULLET, bulletText="•")


def code(text):
    safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return Paragraph(f'<font face="Courier">{safe}</font>', CODE)


def hr():
    return Spacer(1, 6)


def small_table(rows, col_widths=None, header=True):
    style = TableStyle(
        [
            ("FONTNAME", (0, 0), (-1, -1), "Helvetica"),
            ("FONTSIZE", (0, 0), (-1, -1), 9),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("LEFTPADDING", (0, 0), (-1, -1), 6),
            ("RIGHTPADDING", (0, 0), (-1, -1), 6),
            ("TOPPADDING", (0, 0), (-1, -1), 4),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, LIGHT_GREY]),
            ("GRID", (0, 0), (-1, -1), 0.25, MED_GREY),
        ]
    )
    if header:
        style.add("BACKGROUND", (0, 0), (-1, 0), NAVY)
        style.add("TEXTCOLOR", (0, 0), (-1, 0), colors.white)
        style.add("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold")
    return Table(rows, colWidths=col_widths, style=style, repeatRows=1 if header else 0)


# ─── Cover page ───────────────────────────────────────────────────────────────

def cover_story():
    return [
        Spacer(1, 80 * mm),
        Paragraph("Rapport de sécurité", TITLE),
        Spacer(1, 4 * mm),
        Paragraph("Hermanas — système d'automatisation du poulailler", SUBTITLE),
        Spacer(1, 12 * mm),
        Paragraph(f"Date : {REPORT_DATE}", META),
        Paragraph("Version analysée : 0.8.1 (branche <i>master</i>)", META),
        Paragraph(
            "Périmètre : backend Spring Boot 2.7.18 (Java 11) + frontend Angular 20",
            META,
        ),
        Paragraph(
            "Déploiement : Raspberry Pi Zero, exposé publiquement sur www.hermanas.fr "
            "derrière un reverse-proxy Caddy",
            META,
        ),
        Spacer(1, 50 * mm),
        Paragraph(
            "<i>Document interne — ne pas diffuser à l'extérieur de l'équipe technique.</i>",
            META,
        ),
    ]


# ─── Sections ─────────────────────────────────────────────────────────────────

def section_summary():
    return [
        h1("1. Résumé exécutif"),
        p(
            "Hermanas est une application IoT personnelle : elle automatise un poulailler "
            "(porte motorisée, éclairage, ventilation, caméra) à partir d'un Raspberry Pi Zero. "
            "Bien que le périmètre fonctionnel soit étroit, l'application est exposée "
            "publiquement sur Internet, ce qui justifie une posture de sécurité solide."
        ),
        p(
            "L'audit confirme que les fondamentaux sont en place : authentification "
            "BCrypt, autorisation par rôle explicite, CSRF activé, masquage des données "
            "sensibles dans les réponses, journal d'événements pour l'audit. Les principaux "
            "risques résiduels sont structurels (stack EOL imposée par le matériel Pi Zero) "
            "ou opérationnels (secrets par défaut à override, en-têtes de sécurité délégués au reverse-proxy)."
        ),
        h3("Posture globale"),
        small_table(
            [
                ["Axe", "Niveau", "Commentaire"],
                ["Authentification", "Bon", "BCrypt, session + remember-me, état pending validation"],
                ["Autorisation", "Bon", "Rôles explicites, @PreAuthorize + règles par chemin"],
                ["CSRF / cookies", "Bon", "Token CSRF activé, échange via header"],
                ["En-têtes HTTP", "Moyen", "HSTS/CSP absents — délégués au reverse-proxy"],
                ["Validation entrée", "Moyen", "DTOs + path-traversal OK ; bornes manquantes sur /config"],
                ["Rate limiting", "Faible", "Présent sur shutdown/reboot, absent du login"],
                ["Secrets", "Moyen", "Clés par défaut à override, pas de validation"],
                ["Audit", "Moyen", "Bon sur porte/auth, partiel sur config"],
                ["Dépendances", "Moyen", "Spring Boot 2.7 EOL, contrainte matérielle"],
                ["Exposition réseau", "Bon", "Actuator restreint, photo archive privée"],
            ],
            col_widths=[36 * mm, 26 * mm, 110 * mm],
        ),
        Spacer(1, 8),
        p(
            "<b>Recommandation générale.</b> L'application reste adaptée à son usage actuel — "
            "domestique, à faible enjeu — sous réserve que le reverse-proxy Caddy applique "
            "bien HSTS et HTTPS strict. Trois actions sont prioritaires : ajouter un rate-limit "
            "au login, externaliser systématiquement la clé remember-me, et planifier la "
            "migration du Pi Zero vers un matériel supportant Java 17 (donc Spring Boot 3.x) "
            "pour retrouver l'accès aux correctifs de sécurité."
        ),
    ]


def section_scope():
    return [
        h1("2. Périmètre et méthode"),
        h2("2.1 Architecture analysée"),
        small_table(
            [
                ["Composant", "Version", "Localisation"],
                ["Backend Spring Boot", "2.7.18", "pom.xml"],
                ["JDK", "11", "Contrainte Pi Zero"],
                ["Spring Security", "5.7.x (bundled)", "—"],
                ["MariaDB", "—", "Production"],
                ["H2 (tests)", "—", "Profil gpio-fake"],
                ["Frontend Angular", "20.x", "frontend/package.json"],
                ["WebSocket (STOMP)", "—", "websocket/"],
                ["Reverse proxy", "Caddy", "Hors dépôt"],
            ],
            col_widths=[60 * mm, 38 * mm, 70 * mm],
        ),
        h2("2.2 Méthode"),
        p(
            "Revue statique du code source sur la branche <i>master</i> au "
            f"{REPORT_DATE}. Inspection des contrôleurs REST, de la configuration "
            "Spring Security, des annotations <font face=\"Courier\">@PreAuthorize</font>, "
            "des entités JPA, du <font face=\"Courier\">application.properties</font>, "
            "des règles de routage Angular et des gardes. Pas de scan dynamique ni de "
            "test d'intrusion : ce rapport reflète l'état du code, pas l'état du déploiement."
        ),
        h2("2.3 Légende des niveaux de risque"),
        small_table(
            [
                ["Niveau", "Définition"],
                ["Critique", "Exploit direct possible, impact majeur"],
                ["Élevé", "Exploit conditionnel ou impact significatif"],
                ["Moyen", "Risque réel mais limité par d'autres contrôles"],
                ["Faible", "Best-practice non respectée, impact résiduel"],
                ["Info", "Observation neutre / décision documentée"],
            ],
            col_widths=[28 * mm, 140 * mm],
        ),
    ]


def section_authn():
    return [
        h1("3. Authentification"),
        h2("3.1 Points forts"),
        li("<b>Hachage BCrypt</b> — <font face=\"Courier\">BCryptPasswordEncoder</font> "
           "déclaré en bean (<i>SecurityConfig.java:228</i>). Algorithme standard, "
           "salage automatique, coût par défaut (10)."),
        li("<b>Stockage en base</b> — <font face=\"Courier\">DbUserDetailsService</font> "
           "lit les comptes depuis la table <font face=\"Courier\">users</font>. Bootstrap "
           "depuis <font face=\"Courier\">users.properties</font> au premier démarrage, puis "
           "table de référence."),
        li("<b>État compte « non validé »</b> — rôle "
           "<font face=\"Courier\">NOT_VALIDATED_YET</font> et "
           "<font face=\"Courier\">PendingValidationUserDetailsChecker</font> empêchent la "
           "connexion tant qu'un administrateur n'a pas promu le compte. Évite l'auto-enrôlement public."),
        li("<b>Session + remember-me persistant</b> — cookie session "
           "<font face=\"Courier\">JSESSIONID</font> ; cookie remember-me "
           "<font face=\"Courier\">hermanas-remember-me</font> avec tokens stockés en base "
           "(<font face=\"Courier\">persistent_logins</font>, rotation à chaque réutilisation). "
           "Validité 31 jours (<i>application.properties:205</i>)."),
        li("<b>Journalisation des connexions</b> — succès et échecs enregistrés avec "
           "login tenté, raison et IP cliente "
           "(<i>SecurityConfig.java:167-190</i>). Permet la détection a posteriori "
           "de brute-force depuis la page Journalisation."),
        h2("3.2 Risques"),
        small_table(
            [
                ["Risque", "Sévérité", "Référence"],
                [
                    "Pas de rate-limit sur /api/v1/auth/login — brute-force "
                    "possible (atténué par BCrypt lent mais non bloqué)",
                    "Élevé",
                    "SecurityConfig.java:90",
                ],
                [
                    "Pas de rate-limit sur /api/v1/auth/register — possibilité "
                    "de remplir la table users via spam (rôle NOT_VALIDATED_YET)",
                    "Moyen",
                    "SecurityConfig.java:91",
                ],
                [
                    "Cookie remember-me non secure par défaut "
                    "(setUseSecureCookie(false)) — risque en dev HTTP, "
                    "neutralisé en prod si Caddy force HTTPS",
                    "Moyen",
                    "SecurityConfig.java:280",
                ],
                [
                    "Réponse 401 différenciée pour comptes en attente de validation "
                    "(JSON {error:ACCOUNT_PENDING_VALIDATION}) — révèle l'existence "
                    "du login",
                    "Faible",
                    "SecurityConfig.java:183-190",
                ],
                [
                    "Clé remember-me par défaut « change-me-in-production-please » "
                    "dans application.properties — doit impérativement être surchargée",
                    "Élevé",
                    "application.properties:203",
                ],
            ],
            col_widths=[88 * mm, 22 * mm, 58 * mm],
        ),
        h2("3.3 Recommandations"),
        li("Ajouter <font face=\"Courier\">@RateLimited</font> sur "
           "<font face=\"Courier\">/api/v1/auth/login</font> et "
           "<font face=\"Courier\">/api/v1/auth/register</font> (mécanisme déjà disponible "
           "dans <font face=\"Courier\">security/ratelimit/</font>). Cible : 5 tentatives "
           "par IP et par minute."),
        li("Vérifier en démarrage que la clé remember-me a été surchargée (échec rapide "
           "si la valeur par défaut est détectée). Au minimum, un log <i>WARN</i> au boot."),
        li("Aligner le code 401 du compte en attente de validation sur le 401 générique "
           "et différencier le message uniquement après la première connexion réussie."),
    ]


def section_authz():
    return [
        h1("4. Autorisation"),
        h2("4.1 Modèle de rôles"),
        small_table(
            [
                ["Rôle", "Définition"],
                ["USER", "Compte authentifié standard — accès à la caméra (photos), "
                         "actions sur la porte/lampe/ventilo/musique"],
                ["ADMIN", "Tout USER + administration : reboot/shutdown, /actuator/**, "
                          "/logs/**, /events/auth/**, gestion utilisateurs"],
                ["NOT_VALIDATED_YET", "Placeholder pour compte fraîchement enregistré — "
                                       "aucune permission, login refusé"],
            ],
            col_widths=[40 * mm, 128 * mm],
        ),
        h2("4.2 Règles d'autorisation"),
        p(
            "<b>Backend.</b> Combinaison de règles par chemin "
            "(<font face=\"Courier\">antMatchers</font> dans "
            "<font face=\"Courier\">SecurityConfig#filterChain</font>) et d'annotations "
            "<font face=\"Courier\">@PreAuthorize</font> au niveau méthode. La politique "
            "par défaut est <i>permitAll</i> ; chaque chemin sensible est explicitement protégé."
        ),
        small_table(
            [
                ["Chemin / action", "Politique"],
                ["GET /api/v1/auth/me", "permitAll"],
                ["POST /api/v1/auth/{login,logout,register}", "permitAll"],
                ["GET /api/v1/push/vapid-public-key", "permitAll"],
                ["POST /api/v1/push/test", "ADMIN"],
                ["GET /actuator/health, /actuator/info", "permitAll"],
                ["/actuator/** (autres)", "ADMIN"],
                ["/api/v1/logs/**", "ADMIN"],
                ["/api/v1/events/auth/**", "ADMIN"],
                ["/api/v1/buttons/**, /api/v1/email/**", "ADMIN"],
                ["/api/v1/camera/photos/**", "USER ou ADMIN"],
                ["POST/PUT/DELETE/PATCH /api/v1/**", "USER ou ADMIN"],
                ["GET door/turn*, fan/switch, music/switch, …", "USER ou ADMIN"],
                ["Reste (GET API + SPA + Swagger + ws)", "permitAll"],
            ],
            col_widths=[100 * mm, 68 * mm],
        ),
        p(
            "<b>Frontend.</b> Trois mécanismes :"
        ),
        li("<font face=\"Courier\">AuthGuard</font> sur /camera et /notification"),
        li("<font face=\"Courier\">AdminGuard</font> sur /energy"),
        li("Conditions <font face=\"Courier\">@if (isAdmin())</font> dans les templates "
           "(par ex. side-nav, page Système, page Caméra) — défense en profondeur, "
           "complément du backend"),
        h2("4.3 Risques"),
        small_table(
            [
                ["Risque", "Sévérité", "Référence"],
                [
                    "Pas de hiérarchie de rôles — un ADMIN ne récupère pas "
                    "USER automatiquement. Toutes les règles utilisent hasAnyRole pour compenser ; "
                    "facile d'oublier le couple",
                    "Faible",
                    "SecurityConfig.java:131-134",
                ],
                [
                    "Promotion forcée du compte « marguerite » en ADMIN à chaque démarrage "
                    "depuis users.properties — surcharge une modification BDD volontaire",
                    "Info",
                    "DbUserDetailsService.java:77-79",
                ],
                [
                    "Aucune protection contre l'IDOR sur GET /api/v1/residents/{id} "
                    "(à confirmer) — tous les utilisateurs authentifiés voient toutes les résidentes",
                    "Info",
                    "ResidentRestController",
                ],
            ],
            col_widths=[88 * mm, 22 * mm, 58 * mm],
        ),
    ]


def section_csrf_headers():
    return [
        h1("5. CSRF, CORS et en-têtes HTTP"),
        h2("5.1 CSRF"),
        p(
            "CSRF est <b>activé</b> via "
            "<font face=\"Courier\">CookieCsrfTokenRepository.withHttpOnlyFalse()</font> "
            "(<i>SecurityConfig.java:85</i>). Le token est exposé dans le cookie "
            "<font face=\"Courier\">XSRF-TOKEN</font> ; Angular l'ajoute "
            "automatiquement à chaque requête mutante dans l'en-tête "
            "<font face=\"Courier\">X-XSRF-TOKEN</font>. Schéma idiomatique pour une "
            "SPA même-origine."
        ),
        h2("5.2 CORS"),
        p(
            "Aucune origine CORS n'est autorisée par défaut "
            "(<font face=\"Courier\">cors.origins.allowed</font> vide, "
            "<i>application.properties:217</i>). Le SPA Angular étant bundlé dans le JAR "
            "et servi same-origin, ce choix est correct. Le canal WebSocket STOMP applique "
            "la même configuration (<i>WebSocketConfig.java</i>)."
        ),
        h2("5.3 En-têtes de sécurité"),
        small_table(
            [
                ["En-tête", "Statut", "Commentaire"],
                ["X-Frame-Options", "Désactivé volontairement", "frameOptions().disable() — permet l'embarquement de cartes/charts ; expose à du clickjacking si reverse-proxy ne compense pas"],
                ["X-Content-Type-Options", "Non configuré côté app", "Souvent appliqué par Caddy"],
                ["Strict-Transport-Security (HSTS)", "Non configuré côté app", "Doit être ajouté côté Caddy"],
                ["Content-Security-Policy", "Non configuré", "Image distante (board Pi) charge depuis othermod.com ; CSP minimaliste à définir"],
                ["Referrer-Policy", "Non configuré", "À ajouter côté Caddy ou via WebMvcConfigurer"],
            ],
            col_widths=[48 * mm, 38 * mm, 82 * mm],
        ),
        h2("5.4 Recommandations"),
        li("Documenter dans le README la configuration Caddy attendue (HSTS, "
           "X-Content-Type-Options, Referrer-Policy, redirection 80→443)."),
        li("Définir une CSP minimaliste au niveau Spring "
           "(<font face=\"Courier\">SecurityFilterChain</font> ▸ "
           "<font face=\"Courier\">contentSecurityPolicy(\"default-src 'self'; "
           "img-src 'self' https://othermod.com data:; …\")</font>) pour ne pas dépendre "
           "uniquement de Caddy."),
        li("Réactiver X-Frame-Options sur les endpoints non concernés par "
           "l'embarquement, ou passer en <font face=\"Courier\">SAMEORIGIN</font>."),
    ]


def section_input_validation():
    return [
        h1("6. Validation des entrées"),
        h2("6.1 Points forts"),
        li("<b>Bean Validation</b> appliquée sur les contrôleurs critiques : "
           "<font face=\"Courier\">@Valid @RequestBody</font> sur "
           "<font face=\"Courier\">ResidentRequest</font>, "
           "<font face=\"Courier\">UserCreateRequest</font> ; "
           "<font face=\"Courier\">@Min</font>/<font face=\"Courier\">@Max</font> sur "
           "les calibrations servo et le nombre de lignes de logs."),
        li("<b>GlobalExceptionHandler</b> convertit "
           "<font face=\"Courier\">MethodArgumentNotValidException</font> et "
           "<font face=\"Courier\">ConstraintViolationException</font> en JSON structuré "
           "(400 Bad Request) sans fuiter la stack-trace."),
        li("<b>Upload de fichiers</b> : limite 5 Mo par fichier "
           "(<font face=\"Courier\">spring.servlet.multipart.max-file-size = 5MB</font>, "
           "<i>application.properties:193</i>). MIME whitelist "
           "{jpeg, png, webp} dans "
           "<font face=\"Courier\">ResidentPhotoStorage</font>. Redimensionnement client "
           "à ~300 ko avant envoi."),
        li("<b>Protection path traversal</b> centralisée : "
           "<font face=\"Courier\">ResidentPhotoStorage.resolve()</font> et "
           "<font face=\"Courier\">LogsService.resolveSafe()</font> normalisent puis "
           "vérifient <font face=\"Courier\">startsWith</font> sur le répertoire racine."),
        li("<b>Pas d'injection SQL</b> détectée — usage exclusif de Spring Data JPA "
           "avec requêtes paramétrées."),
        h2("6.2 Risques"),
        small_table(
            [
                ["Risque", "Sévérité", "Référence"],
                [
                    "Endpoints /api/v1/config/** acceptent des paramètres "
                    "@RequestParam sans bornes — durées négatives, positions servo hors "
                    "plage, ports SMTP > 65535 sont acceptés et peuvent rendre l'app instable",
                    "Moyen",
                    "ConfigRestController.java",
                ],
                [
                    "MIME validation basée sur le header Content-Type de la requête, "
                    "facile à falsifier — pas de magic-byte check ni de re-encodage",
                    "Faible",
                    "ResidentPhotoStorage.java:57-60",
                ],
                [
                    "Le viewer de logs autorise tout fichier matchant des "
                    "patterns .log* dans le répertoire ; un fichier déposé manuellement "
                    "(ex. dump) y serait lisible par tout admin via l'UI",
                    "Faible",
                    "LogsService.java:161-162",
                ],
            ],
            col_widths=[88 * mm, 22 * mm, 58 * mm],
        ),
        h2("6.3 Recommandations"),
        li("Ajouter <font face=\"Courier\">@Min/@Max</font> sur tous les setters de "
           "ConfigRestController (port 1-65535, durées 1-60000 ms, positions servo 1-100, etc.)."),
        li("Compléter la validation upload par une lecture des magic bytes (au moins JPEG SOI 0xFFD8FFE0)."),
        li("Restreindre le viewer de logs à une whitelist explicite de noms "
           "(<font face=\"Courier\">spring.log</font> et ses rotations seulement)."),
    ]


def section_data_exposure():
    return [
        h1("7. Exposition des données"),
        h2("7.1 Données sensibles masquées"),
        li("<b>Mot de passe SMTP</b> — exposé via "
           "<font face=\"Courier\">password_set: true|false</font>, jamais la valeur "
           "(<i>ConfigRestController.java:190</i>)."),
        li("<b>Clé API OpenWeather</b> — exposée via "
           "<font face=\"Courier\">key_set</font> + "
           "<font face=\"Courier\">key_length</font>, jamais la valeur "
           "(<i>ConfigRestController.java:170-172</i>)."),
        li("<b>Coordonnées GPS</b> du poulailler — non retournées dans "
           "<font face=\"Courier\">GET /api/v1/config</font>, modifiables uniquement "
           "via les endpoints PUT dédiés (<i>location/latitude</i> et "
           "<i>location/longitude</i>). Décision explicite documentée dans "
           "<font face=\"Courier\">WeatherSettings</font>."),
        li("<b>DTOs en frontière</b> — ResidentDTO, UserDTO, SensorDTO, EventDTO. "
           "Les entités JPA ne sont jamais retournées telles quelles."),
        li("<b>Stack-traces</b> — masquées par GlobalExceptionHandler (500 générique "
           "côté client, détails uniquement dans les logs serveur)."),
        h2("7.2 Risques résiduels"),
        small_table(
            [
                ["Risque", "Sévérité", "Référence"],
                [
                    "Le journal métier (/api/v1/events/business) est public et expose "
                    "l'historique des ouvertures de porte, allumages de lampe, etc. — "
                    "permet de modéliser la présence des animaux et indirectement de "
                    "l'opérateur",
                    "Moyen",
                    "SecurityConfig.java + EventsRestController",
                ],
                [
                    "/actuator/health est public et révèle l'état des composants "
                    "(disque plein, capteur HS, porte indéterminée)",
                    "Faible",
                    "SecurityConfig.java:104-105",
                ],
                [
                    "Filename des photos résidentes retourné (UUID désormais), mais "
                    "la liste complète des résidents est lisible par tout authentifié "
                    "— pas de portée par utilisateur",
                    "Info",
                    "ResidentDTO",
                ],
            ],
            col_widths=[88 * mm, 22 * mm, 58 * mm],
        ),
    ]


def section_audit():
    return [
        h1("8. Journalisation et audit"),
        h2("8.1 Couverture du journal d'événements"),
        p(
            "L'énumération <font face=\"Courier\">EventType</font> couvre les principales "
            "actions métier :"
        ),
        small_table(
            [
                ["Catégorie", "Événements"],
                ["Authentification", "LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT"],
                ["Porte", "DOOR_OPENED, DOOR_CLOSED, DOOR_OPEN_FAILED, DOOR_CLOSE_FAILED, DOOR_POSITION_UNKNOWN"],
                ["Périphériques", "LIGHT_ON/OFF, FAN_ON/OFF, MUSIC_STARTED/STOPPED, COCORICO"],
                ["Résidentes", "RESIDENT_CREATED, RESIDENT_DELETED"],
                ["Système", "STARTUP, SHUTDOWN, SHUTDOWN_REQUESTED, REBOOT_REQUESTED"],
            ],
            col_widths=[40 * mm, 128 * mm],
        ),
        h2("8.2 Annotation @AuditLog"),
        p(
            "Une annotation maison <font face=\"Courier\">@AuditLog(category, operation)</font> "
            "permet de tracer les opérations sensibles. Elle est aujourd'hui posée sur "
            "les actions porte (ouverture/fermeture) et système (reboot/shutdown). "
            "<b>Couverture incomplète</b> : les changements de configuration (SMTP, GPS, "
            "URL d'inférence IA, clé OpenWeather) ne génèrent qu'un log INFO standard."
        ),
        h2("8.3 Risques"),
        small_table(
            [
                ["Risque", "Sévérité", "Référence"],
                [
                    "Modifications de configuration non auditées explicitement — "
                    "un admin peut changer la cible SMTP sans trace dédiée",
                    "Moyen",
                    "ConfigRestController.java",
                ],
                [
                    "EventType stocké par ordinal — un réordonnancement de l'enum "
                    "réécrirait l'interprétation des historiques",
                    "Moyen",
                    "EventType.java (commentaire d'avertissement présent)",
                ],
                [
                    "Pas de rotation par taille — le fichier spring.log peut "
                    "saturer le disque du Pi Zero avant la rotation quotidienne",
                    "Faible",
                    "application.properties (logging.*)",
                ],
            ],
            col_widths=[88 * mm, 22 * mm, 58 * mm],
        ),
        h2("8.4 Recommandations"),
        li("Étendre <font face=\"Courier\">@AuditLog</font> à tous les setters de "
           "<font face=\"Courier\">ConfigRestController</font>, "
           "<font face=\"Courier\">EnergyRestController</font>, "
           "<font face=\"Courier\">UserRestController</font> (catégories CONFIG, ENERGY, USER)."),
        li("Migrer EventType vers une persistance par <font face=\"Courier\">name()</font> "
           "(<font face=\"Courier\">@Enumerated(EnumType.STRING)</font>) — coût ponctuel : "
           "migration de la colonne, gain : robustesse à long terme."),
        li("Activer une rotation par taille (Logback : "
           "<font face=\"Courier\">SizeAndTimeBasedRollingPolicy</font>, "
           "10 Mo / fichier, conservation 7 jours)."),
    ]


def section_secrets():
    return [
        h1("9. Gestion des secrets"),
        h2("9.1 État actuel"),
        li("<b>users.properties</b> exclus du dépôt "
           "(<i>.gitignore</i> ligne 36) ; chemin configurable via "
           "<font face=\"Courier\">hermanas.security.users-file</font> "
           "(défaut <font face=\"Courier\">./users.properties</font> à côté du JAR)."),
        li("<b>CLI de génération de hash</b> intégrée : "
           "<font face=\"Courier\">java -jar hermanas.jar --hash &lt;password&gt;</font> — "
           "évite de coller le mot de passe en clair dans un terminal externe."),
        li("<b>Setters d'admin</b> pour SMTP, OpenWeather, etc. — la valeur écrite est "
           "stockée en BDD via Parameter et n'apparaît jamais dans une réponse REST."),
        h2("9.2 Risques"),
        small_table(
            [
                ["Risque", "Sévérité", "Référence"],
                [
                    "Clé remember-me « change-me-in-production-please » par défaut, "
                    "sans validation de remplacement",
                    "Élevé",
                    "application.properties:203",
                ],
                [
                    "Placeholders de configuration SMTP et clé OpenWeather "
                    "exposés en clair dans application.properties (valeurs sentinelles), "
                    "à override en environnement",
                    "Info",
                    "application.properties:124, 142-146",
                ],
                [
                    "Pas d'intégration coffre-fort (Vault, AWS Secrets Manager, "
                    "Spring Cloud Config) — les secrets de prod vivent dans "
                    "/home/pi/application.properties ou en BDD",
                    "Info",
                    "Choix d'architecture",
                ],
            ],
            col_widths=[88 * mm, 22 * mm, 58 * mm],
        ),
        h2("9.3 Recommandations"),
        li("Bloquer le démarrage si "
           "<font face=\"Courier\">hermanas.security.remember-me-key</font> "
           "est la valeur par défaut (échec rapide explicite, ou minimum log WARN)."),
        li("Sortir <font face=\"Courier\">spring.mail.password</font> de "
           "<font face=\"Courier\">application.properties</font> : ne plus laisser de "
           "placeholder en clair, n'accepter que l'override BDD ou variable d'environnement."),
        li("Tourner la clé remember-me en cas de fuite supposée (invalide tous les "
           "tokens persistant_logins existants — comportement intentionnel)."),
    ]


def section_rate_limit():
    return [
        h1("10. Rate-limiting et abuse"),
        p(
            "Une implémentation maison (package "
            "<font face=\"Courier\">security/ratelimit/</font>) propose une annotation "
            "<font face=\"Courier\">@RateLimited(maxRequests, perSeconds)</font> "
            "appliquée par un aspect. Le compteur est en mémoire (par IP), reset au "
            "redémarrage — adapté au Pi Zero (instance unique)."
        ),
        h2("Couverture actuelle"),
        small_table(
            [
                ["Endpoint", "Limite"],
                ["POST /api/v1/system/shutdown", "2 requêtes / 5 min / IP"],
                ["POST /api/v1/system/reboot", "2 requêtes / 5 min / IP"],
                ["POST /api/v1/auth/login", "Aucune"],
                ["POST /api/v1/auth/register", "Aucune"],
                ["Reste", "Aucune"],
            ],
            col_widths=[100 * mm, 68 * mm],
        ),
        h2("Recommandations"),
        li("Étendre la limitation à /api/v1/auth/login (5 essais / minute / IP)."),
        li("Étendre à /api/v1/auth/register (3 enregistrements / heure / IP)."),
        li("Ajouter un bandeau d'avertissement et un log AUDIT lorsqu'un seuil est atteint."),
        li("Documenter le comportement attendu derrière reverse-proxy (clé du compteur = "
           "<font face=\"Courier\">X-Forwarded-For</font>, déjà géré côté code)."),
    ]


def section_dependencies():
    return [
        h1("11. Dépendances et chaîne d'approvisionnement"),
        h2("11.1 Backend"),
        small_table(
            [
                ["Composant", "Version", "Statut"],
                ["Java", "11", "LTS — fin de support septembre 2026"],
                ["Spring Boot", "2.7.18", "EOL novembre 2023 — bloqué par Pi Zero"],
                ["Spring Security", "5.7.x (bundled)", "EOL avec Boot 2.7"],
                ["@EnableGlobalMethodSecurity", "Déprécié", "Remplacement requiert Boot 3.x"],
                ["javax.* namespaces", "Legacy", "Migration jakarta.* impossible sur Boot 2.7"],
                ["Pi4j", "≤ 2.4.0", "Pin matériel — versions ultérieures incompatibles Pi Zero+Java 11"],
                ["MariaDB Connector/J", "3.5.8", "À jour"],
                ["BouncyCastle", "1.70", "Récent"],
                ["commons-io", "2.18.0", "Récent"],
            ],
            col_widths=[55 * mm, 38 * mm, 75 * mm],
        ),
        h2("11.2 Frontend"),
        small_table(
            [
                ["Composant", "Version", "Statut"],
                ["Angular", "20.x", "Récent, supporté"],
                ["RxJS", "7.8.1", "Récent"],
                ["Bootstrap", "5.3.3", "Récent"],
                ["aws-amplify et libs associées", "Retirées", "Migration phase 7 — 310 paquets désinstallés"],
            ],
            col_widths=[55 * mm, 38 * mm, 75 * mm],
        ),
        h2("11.3 Risque structurel"),
        p(
            "Le stack backend est dans une fenêtre de risque qui ne se résoudra "
            "qu'avec un upgrade matériel. Tant que le Pi Zero est en service :"
        ),
        li("Pas de patches de sécurité officiels Spring Boot 2.7 — "
           "une CVE Boot/Security future ne sera pas corrigée par l'éditeur."),
        li("Surveillance manuelle requise (NVD, advisories Spring, snyk advisor)."),
        li("Pas de migration jakarta.* possible — toute dépendance qui basculera vers "
           "Jakarta EE 9+ deviendra inutilisable sans rétro-portage."),
        h2("11.4 Recommandations"),
        li("Lancer périodiquement <font face=\"Courier\">mvn dependency-check:check</font> "
           "(OWASP Dependency-Check, gratuit) ; configurer un seuil d'alerte à CVSS ≥ 7."),
        li("Planifier l'upgrade matériel vers un Raspberry Pi 4/5 pour rester sur Boot 3.x. "
           "Échéance recommandée : avant le 31 décembre 2026."),
        li("Pour le frontend : <font face=\"Courier\">npm audit --production</font> en CI."),
    ]


def section_operational():
    return [
        h1("12. Opérationnel et réseau"),
        h2("12.1 Endpoints d'administration"),
        p(
            "Les endpoints Actuator sont restreints à un sous-ensemble : "
            "<font face=\"Courier\">health, info, metrics, loggers, prometheus</font> "
            "(<i>application.properties:22</i>). Tous les endpoints potentiellement "
            "dangereux sont explicitement désactivés : "
        ),
        code(
            "management.endpoint.heapdump.enabled=false\n"
            "management.endpoint.threaddump.enabled=false\n"
            "management.endpoint.env.enabled=false\n"
            "management.endpoint.configprops.enabled=false\n"
            "management.endpoint.beans.enabled=false\n"
            "management.endpoint.shutdown.enabled=false"
        ),
        p(
            "<font face=\"Courier\">/actuator/health</font> et "
            "<font face=\"Courier\">/actuator/info</font> sont publics ; le reste "
            "exige le rôle ADMIN."
        ),
        h2("12.2 HTTPS et reverse-proxy"),
        p(
            "Le code suppose un déploiement derrière Caddy : il fait confiance à "
            "<font face=\"Courier\">X-Forwarded-For</font> pour la journalisation IP "
            "(<i>SecurityConfig.java:217</i>) et désactive le flag secure du "
            "cookie remember-me pour permettre le dev HTTP. <b>La configuration Caddy "
            "n'est pas dans ce dépôt</b> et doit être documentée à part : redirection "
            "80→443, HSTS, headers de sécurité, blocage des en-têtes X-Forwarded-* en "
            "entrée pour éviter le spoofing."
        ),
        h2("12.3 Système de fichiers"),
        small_table(
            [
                ["Chemin", "Contenu", "Permissions"],
                ["./residents-photos/", "Photos uploadées", "Hérité de l'umask Linux"],
                ["./log/", "Logs applicatifs", "Hérité de l'umask Linux"],
                ["./photos/", "Captures caméra", "Hérité de l'umask Linux"],
                ["./users.properties", "Hashes bcrypt (bootstrap)", "À sécuriser (chmod 600)"],
                ["./application.properties", "Surcharges runtime", "À sécuriser (chmod 600)"],
            ],
            col_widths=[55 * mm, 60 * mm, 53 * mm],
        ),
        h2("12.4 Exécution de commandes externes"),
        p(
            "L'application invoque des binaires shell pour la caméra "
            "(<font face=\"Courier\">camera.streaming.command</font>), le lecteur audio "
            "(<font face=\"Courier\">music.player.start.cmd</font>) et le mixer "
            "(<font face=\"Courier\">music.volume.cmd</font>). Les chemins sont "
            "définis dans <font face=\"Courier\">application.properties</font> ; ils "
            "ne reçoivent <b>pas</b> d'input utilisateur, donc pas d'injection de commande "
            "côté API. Garder cette discipline : ne jamais concaténer une valeur "
            "@RequestParam dans une commande shell."
        ),
        h2("12.5 Recommandations"),
        li("Versionner la configuration Caddy à côté du repo (sous-dossier infra/) ou la "
           "documenter dans le README."),
        li("Appliquer <font face=\"Courier\">chmod 600</font> et "
           "<font face=\"Courier\">chown pi:pi</font> sur "
           "<font face=\"Courier\">users.properties</font> et la surcharge "
           "<font face=\"Courier\">application.properties</font> de la Pi."),
        li("Vérifier dans la configuration Caddy que les en-têtes "
           "<font face=\"Courier\">X-Forwarded-*</font> entrants sont écrasés "
           "(<font face=\"Courier\">header_up X-Forwarded-For {http.request.remote.host}</font>)."),
    ]


def section_action_plan():
    return [
        h1("13. Plan d'actions priorisé"),
        h2("13.1 Priorité 1 — à traiter sous 1 mois"),
        small_table(
            [
                ["Action", "Effort", "Risque adressé"],
                ["Rate-limit sur /auth/login (5/min/IP)", "S", "Brute-force"],
                ["Rate-limit sur /auth/register (3/h/IP)", "S", "Spam comptes"],
                ["Fail-fast si remember-me-key est la valeur par défaut", "XS", "Tokens prédictibles"],
                ["Documenter / versionner la conf Caddy (HSTS, CSP, headers)", "S", "Headers de sécurité"],
            ],
            col_widths=[88 * mm, 18 * mm, 62 * mm],
        ),
        h2("13.2 Priorité 2 — à traiter sous 3 mois"),
        small_table(
            [
                ["Action", "Effort", "Risque adressé"],
                ["Bornes @Min/@Max sur tous les setters Config", "M", "Stabilité / abus"],
                ["@AuditLog sur les setters Config / User", "S", "Audit incomplet"],
                ["Migrer EventType vers @Enumerated(STRING)", "M", "Robustesse historique"],
                ["Rotation logs par taille (SizeAndTimeBased)", "S", "Saturation disque Pi"],
                ["Magic-byte check sur upload photos", "S", "Bypass MIME"],
                ["CSP minimaliste côté Spring", "S", "Défense en profondeur"],
            ],
            col_widths=[88 * mm, 18 * mm, 62 * mm],
        ),
        h2("13.3 Priorité 3 — stratégique (6-12 mois)"),
        small_table(
            [
                ["Action", "Effort", "Risque adressé"],
                ["Upgrade matériel Pi 4/5 → Boot 3.x + Java 21", "XL", "Stack EOL"],
                ["OWASP Dependency-Check en CI", "M", "Veille CVE"],
                ["npm audit en CI", "S", "Veille front"],
                ["Test d'intrusion externe", "L", "Validation black-box"],
            ],
            col_widths=[88 * mm, 18 * mm, 62 * mm],
        ),
        h2("13.4 Légende effort"),
        p("XS &lt; 2 h · S = 1/2 j · M = 1-2 j · L = 1 semaine · XL = projet"),
    ]


def section_conclusion():
    return [
        h1("14. Conclusion"),
        p(
            "Hermanas présente une posture de sécurité <b>raisonnable</b> au regard de "
            "son périmètre et de sa cible (instance unique, usage personnel, exposition "
            "publique limitée au site vitrine et au tableau de bord)."
        ),
        p(
            "Les fondamentaux essentiels — authentification BCrypt, autorisation explicite "
            "par rôle, CSRF, masquage des secrets dans les réponses, journal d'événements "
            "— sont correctement mis en œuvre. Les faiblesses identifiées sont soit "
            "structurelles (stack EOL imposée par le matériel), soit opérationnelles "
            "(secrets par défaut, en-têtes délégués à Caddy), soit incrémentales (validation "
            "des bornes, audit plus large)."
        ),
        p(
            "Les trois priorités à court terme — ajouter un rate-limit au login, durcir "
            "la clé remember-me et documenter la configuration Caddy — sont à faible coût "
            "et fortement amélioratives. La priorité stratégique reste le renouvellement "
            "matériel du Pi Zero pour sortir de la dette Spring Boot 2.7 / Java 11."
        ),
        Spacer(1, 12),
        p(
            "<i>Ce rapport reflète l'état du code au "
            f"{REPORT_DATE}. À renouveler à chaque évolution majeure (rôle, "
            "endpoint sensible, mise à jour de dépendance critique) ou au minimum "
            "une fois par an.</i>"
        ),
    ]


# ─── Main ─────────────────────────────────────────────────────────────────────

def main():
    out_path = Path("docs/security-report/hermanas-security-report.pdf")
    out_path.parent.mkdir(parents=True, exist_ok=True)

    doc = build_doc(out_path)
    story = []

    # Cover
    story += cover_story()
    story.append(NextPageTemplate("body"))
    story.append(PageBreak())

    # Body sections
    for section in [
        section_summary(),
        section_scope(),
        section_authn(),
        section_authz(),
        section_csrf_headers(),
        section_input_validation(),
        section_data_exposure(),
        section_audit(),
        section_secrets(),
        section_rate_limit(),
        section_dependencies(),
        section_operational(),
        section_action_plan(),
        section_conclusion(),
    ]:
        story += section

    doc.build(story)
    print(f"Wrote {out_path} ({out_path.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
