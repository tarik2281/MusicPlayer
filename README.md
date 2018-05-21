# MusicPlayer

Dieses Projekt ist eine Android App zum Abspielen von Musik über lokale Dateien auf dem Gerät. Die Musikdateien werden in einer Bibliothek verwaltet, die von einer SQLite Datenbank gestützt wird. Mithilfe dieser Bibliothek können Songs nach dem Interpret, dem Album, etc. sortiert und angezeigt werden. Die Musikdateien werden mit der [FFmpeg](https://www.ffmpeg.org/)-Bibliothek dekodiert und mithilfe der AudioTrack-Klasse des Android Frameworks abgespielt. Man kann auch die Metadaten der einzelnen Dateien einsehen und bearbeiten. Dies wird mithilfe der [TagLib](http://taglib.org/)-Bibliothek realisiert.
Im Binaries-Ordner befindet sich eine APK-Datei zum Installieren und Ausführen dieser App.
