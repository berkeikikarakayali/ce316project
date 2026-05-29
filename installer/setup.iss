[Setup]
AppName=IAE - Integrated Assignment Environment
AppVersion=1.0
AppPublisher=CE316 Team 7
DefaultDirName={autopf}\IAE
DefaultGroupName=IAE
OutputDir=.
OutputBaseFilename=IAE_Setup
Compression=lzma
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"

[Files]
Source: "..\build\libs\IAE-1.0.jar"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\IAE"; Filename: "{app}\IAE-1.0.jar"
Name: "{autodesktop}\IAE"; Filename: "{app}\IAE-1.0.jar"; Tasks: desktopicon

[Run]
Filename: "{app}\IAE-1.0.jar"; Description: "Launch IAE"; Flags: nowait postinstall skipifsilent shellexec
