package org.jibe77.hermanas.service.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garde-fou sur les annotations {@link CacheEvict} de {@link ConfigService}.
 *
 * <p><b>La régression que ce test empêche.</b> Spring dérive la clé de cache des
 * <em>paramètres</em> de la méthode. Un getter sans argument est donc mis en cache
 * sous {@code SimpleKey.EMPTY}, tandis qu'un {@code @CacheEvict} posé sur
 * {@code setXxx(int valeur)} évince la clé dérivée de {@code valeur} — une entrée
 * qui n'existe pas. La valeur en cache survit à l'écriture, et l'API continue de
 * renvoyer l'ancienne.</p>
 *
 * <p>Constaté le 2026-08-04 : régler la qualité photo de 5 à 10 écrivait bien en
 * base, mais l'endpoint de configuration renvoyait toujours 5. <b>55 setters sur
 * 62</b> étaient concernés — presque toute la configuration.</p>
 *
 * <p>Le remède est {@code allEntries = true} : chaque cache ne contenant qu'une
 * seule entrée, tout vider revient exactement à évincer la bonne clé.</p>
 *
 * <p>Test <em>structurel</em> : il inspecte les annotations plutôt qu'un cas
 * particulier, donc il couvre tous les setters présents et à venir.</p>
 */
class ConfigServiceCacheEvictTest {

    @Test
    @DisplayName("Tout @CacheEvict sur un setter à paramètre doit préciser allEntries ou key")
    void everyCacheEvictOnAParameterisedSetterMustTargetTheRightKey() {
        List<String> broken = new ArrayList<>();

        for (Method method : ConfigService.class.getDeclaredMethods()) {
            CacheEvict evict = method.getAnnotation(CacheEvict.class);
            if (evict == null || method.getParameterCount() == 0) {
                // Sans paramètre, la clé vaut SimpleKey.EMPTY des deux côtés :
                // l'éviction tombe juste, rien à vérifier.
                continue;
            }
            boolean targetsEverything = evict.allEntries();
            boolean hasExplicitKey = !evict.key().isEmpty() || !evict.keyGenerator().isEmpty();
            if (!targetsEverything && !hasExplicitKey) {
                broken.add(method.getName());
            }
        }

        assertTrue(broken.isEmpty(),
                "Ces setters évincent une clé dérivée de leur paramètre, alors que le getter "
                        + "correspondant est mis en cache sous SimpleKey.EMPTY. L'éviction ne "
                        + "porte donc sur rien et la valeur lue reste périmée. Ajouter "
                        + "allEntries = true :\n  " + String.join("\n  ", broken));
    }
}
