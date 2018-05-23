# quenfo
Main code of the BIBB project 


## Classification
### zu ergänzende Dateien

#### in den Ordner classification/data/trainingSets/ <br />

--> eigene trainingsdaten als tsv-File im Format <b>[UUID] [tab] [classID] [new line] [paragraph] [empty line]</b> <br />
--> oder die vorhandenen (jedoch anonymisierten) Trainingsdaten (trainingdata_anonymized.tsv) verwenden. <br />

#### in den Ordner /classification/db/ </br >
Input-Datenbank mit Stellenanzeigen. Es liegt eine leere DB mit den geforderten DB-Schema vor (JobAds_empty.db)

### Ausführen 

main-Class: .../classification/applications/ClassifyDatabase.java <br />

Konfigurations-Variablen ggf. anpassen 


## Information Extration
