# quenfo
Main code of the BIBB project 

## Dokumentation

**Geduldig, Alena (2017)**. *Muster und Musterbildungsverfahren für domänenspezifische
Informationsextraktion Ein Bootstrapping-Ansatz zur Extraktion von Kompetenzen aus Stellenanzeigen*. Masterarbeit. URL: <a href="http://www.spinfo.phil-fak.uni-koeln.de/sites/spinfo/arbeiten/Masterthesis_Alena.pdf">http://www.spinfo.phil-fak.uni-koeln.de/sites/spinfo/arbeiten/Masterthesis_Alena.pdf</a> <br /> <br />

**Geduldig, Alena & Betül Günturk-Kuhl (erscheint 2018)**. *Kompetenzanforderungen in Stellenanzeigen. Die Nutzung maschineller Lernverfahren zur Extraktion und Kategorisierung von Kompetenzen*. <br /><br />

**Hermes, Jürgen & Manuel Schandock (2016)**. "Stellenanzeigenanalyse in der
Qualifikationsentwicklungsforschung. Die Nutzung maschineller Lernverfahren in der Klassifikation von Textabschnitten". In: *Fachbeiträge im Internet. Bundesinstitut für Berufsbildung*. URL: <a href="https://www.bibb.de/veroeffentlichungen/de/publication/show/8146">https://www.bibb.de/veroeffentlichungen/de/publication/show/8146</a>

## Classification (Zoning)
Segmentierung von Stellenanzeigen in Abschnitte und Klassifikation der Abschnitte in die vier Kategorien: <br />

1. Unternehmensbeschreibung<br />
2. Jobbeschreibung<br />
3. Bewerberprofil<br />
4. Sonstiges/Formalia<br />

### zu ergänzende Dateien/Daten

#### in den Ordner classification/data/trainingSets/ <br />

--> eigene trainingsdaten als tsv-File im Format <b>CLASS: [tab] classID [new line] paragraph [empty line]</b> <br />
--> oder die vorhandenen (jedoch anonymisierten) Trainingsdaten (trainingdata_anonymized.tsv) verwenden. <br />

#### in den Ordner classification/db/ </br >
--> Input-Datenbank mit Stellenanzeigen. Es liegt eine leere DB mit den geforderten DB-Schema vor (JobAds_empty.db)

### Application

.../classification/applications/<b>ClassifyDatabase.java</b> <br />

Konfigurations-Variablen ggf. anpassen <br /><br />

## Information Extration 
Extrahiert  Kompetenzen aus den Abschnitten, die zuvor der Klasse 'Bewerberprofil' zugeordnet wurden<br />
Extrahiert Tools aus den Abschnitten, die zuvor der Klasse 'Jobbeschreibung' und/oder 'Bewerberprofil' zugeordnet wurden.<br />

### zu ergänzende Dateien/Daten

#### in den Ordner information_extraction/data/openNLPmodels/ <br />

--> de-sent.bin & de-token.bin (downloadlink: http://opennlp.sourceforge.net/models-1.5/) <br />
--> ger-tagger+lemmatizer+morphology+graph-based-3.6+.tgz (downloadlink: https://code.google.com/archive/p/mate-tools/downloads) <br />

#### in den Ordner information_extraction/data/competences/ <br />

-->  competences.txt (leer, oder mit bereits bekannten/extrahierten Kompetenzausdrücken) <br />
--> noCompetences.txt (leer, oder mit Falschextraktionen, die nicht wiederholt werden sollen) <br />

#### in den Ordner information_extraction/data/tools/ <br />

--> tools.txt (leer, oder mit bereits bekannten/extrahierten Tools) <br />
--> noTools.txt (leer, oder mit Falschextraktionen, die nicht wiederholt werden sollen) 

### Applications

Extraktion von Kompetenzen: .../information_extraction/applications/<b>ExtractNewCompetences.java</b> <br />
Extraktion von Tools: .../information_extraction/applications/<b>ExtractNewTools.java</b> < br/>

Matching (= Extraktion aller Fundstellen/Sätze von Kompetenzen/Tools)  <br />

der extrahierten Kompetenzen: .../information_extraction/applications/<b>MatchNotValidatedCompetences.java</b> <br />
nur der validierten Kompetenzen (competences.txt): .../information_extraction/applications/<b>MatchCompetences.java</b> <br />

der extrahierten Tools: .../information_extraction/applications/<b>MatchNotValidatedTools.java</b> <br />
nur der validierten Tools (tools.txt): .../information_extraction/applications/<b>MatchTools.java</b> <br /><br />

## Categorization

Bildet Gruppen von Kompetenzen/Tools auf Grundlage ihrer Stringähnlichkeit (Needleman-Wunsch) und/oder Kookkurrenzen (chi-Quadrat)

### zu ergänzende Daten
 --> CategorizedCompetences.db (DB mit bereits kategorisierten Kompetenzen - Pfadangabe zur DB in den jeweiligen Apps anpassen) <br />
 --> CategorizedTools.db (DB mit bereits kategorisierten Tools - Pfadangabe zur DB in den jeweiligen Apps anpassen)
 
### Applications

Kategorisierung auf Grundlage von Kookkurrenzen mit bereits kategorisierten Tools/Competences: <br />
.../categorization/applications/<b>GroupCompetencesByCooccurrence.java</b> <br />
.../categorization/applications/<b>GroupToolsByCooccurrence.java</b> <br />

Kategorisierung auf Grundlage von Stringähnlichkeit: <br />
.../categorization/applications/<b>GroupCompetencesByStringSimilarity.java</b> <br />
.../categorization/applications/<b>GroupToolsByStringSimilarity.java</b> <br />

Kategorisierung auf Grundlage von Kookkurrenzen und Stringähnlichkeit: <br />
.../categorization/applications/<b>GroupCompetences.java</b> <br />
.../categorization/applications/<b>GroupTools.java</b> <br />






