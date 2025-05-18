## Prérequis

- Java 17 ou supérieur (le projet utilise des fonctionnalités de Java 23 si disponibles)
- Maven 3.6 ou supérieur

## Compilation

Pour compiler le projet, exécutez la commande suivante dans le répertoire racine :

```bash
mvn clean compile
```

## Exécution

### Lancement de l'Application

Pour exécuter l'application avec Maven :

```bash
mvn exec:java -Dexec.mainClass="algo.transit.ALGOL" -Dexec.args="[arguments]"
```

Remplacez `[arguments]` par les arguments appropriés (voir ci-dessous).

Alternativement, après compilation, vous pouvez exécuter l'application directement avec Java :

```bash
java -cp target/classes algo.transit.ALGOL [arguments]
```

### Arguments de Ligne de Commande

L'application requiert au minimum trois arguments :

```
START_STOP END_STOP START_TIME [OPTIONS]
```

- `START_STOP` : L'identifiant de l'arrêt de départ (ex. : "SNCB-S8891660")
- `END_STOP` : L'identifiant de l'arrêt de destination (ex. : "TEC-X615aya")
- `START_TIME` : L'heure de départ au format HH:MM (ex. : "10:30")

#### Arguments Optionnels

- `--walking-speed <vitesse>` : Définir la vitesse de marche en mètres par minute (par défaut : 80.0)
- `--max-walk-time <temps>` : Définir le temps maximum de marche en minutes (par défaut : 10.0)
- `--forbidden-modes <modes>` : Définir les modes de transport interdits (ex. : BUS, TRAIN)
- `--mode-weights <mode:poids>` : Définir des poids personnalisés pour les modes de transport (ex. : BUS:1.5 TRAIN:0.8)
- `--arrive-by` : Trouver un chemin arrivant à l'heure spécifiée, et non au départ
- `--optimization-goal <objectif>` : Définir l'objectif d'optimisation : time|transfers|walking (par défaut : time)
- `--output-format <format>` : Définir le format de sortie : detailed|summary (par défaut : detailed)
- `--show-stats` : Afficher des statistiques détaillées sur le chemin trouvé
- `--visualize` : Activer la visualisation de l'algorithme de recherche de chemin
- `--help` : Afficher le message d'aide