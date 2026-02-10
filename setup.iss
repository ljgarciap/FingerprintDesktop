[Setup]
AppName=Fingerprint Desktop
AppVersion=1.0
DefaultDirName=C:\FingerprintDesktop
DefaultGroupName=Fingerprint Desktop
OutputBaseFilename=FingerprintDesktopInstaller
Compression=lzma
SolidCompression=yes
SetupIconFile=icon.ico
UninstallDisplayIcon={app}\icon.ico
PrivilegesRequired=admin
DirExistsWarning=no
DisableDirPage=yes
DisableProgramGroupPage=yes

[Files]
Source: "app\*"; DestDir: "{app}\app"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "lib\*"; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "native\*"; DestDir: "{app}\native"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "start.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "icon.ico"; DestDir: "{app}"; Flags: ignoreversion

; Dependencias redistribuibles y driver
Source: "vcredist_x64_2008.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall
Source: "VC_redist.x64.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall
Source: "SgDrvSetupUniversal.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall

[Icons]
Name: "{group}\Fingerprint Desktop"; Filename: "{app}\start.bat"; IconFilename: "{app}\icon.ico"
Name: "{commondesktop}\Fingerprint Desktop"; Filename: "{app}\start.bat"; IconFilename: "{app}\icon.ico"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Crear un acceso directo en el escritorio"; GroupDescription: "Accesos directos"; Flags: unchecked

[Run]
; Instaladores condicionales
Filename: "{tmp}\vcredist_x64_2008.exe"; Parameters: "/quiet /norestart"; Check: NeedsVC2008; StatusMsg: "Instalando Microsoft Visual C++ 2008 Redistributable..."
Filename: "{tmp}\VC_redist.x64.exe"; Parameters: "/quiet /norestart"; Check: NeedsVC2015; StatusMsg: "Instalando Microsoft Visual C++ 2015–2022 Redistributable..."
Filename: "{tmp}\SgDrvSetupUniversal.exe"; Parameters: "/silent"; Check: NeedsSecuGenDriver; StatusMsg: "Instalando controlador SecuGen..."

; Agregar native al PATH del sistema
Filename: "cmd.exe"; Parameters: "/C setx /M PATH ""%PATH%;C:\FingerprintDesktop\native"""; Flags: runhidden

; Ejecutar aplicación al finalizar
Filename: "{app}\start.bat"; Description: "Ejecutar Fingerprint Desktop"; Flags: nowait postinstall skipifsilent

[Code]
var
  ProgressPage: TOutputProgressWizardPage;

function NeedsVC2008(): Boolean;
var
  key: String;
begin
  key := 'SOFTWARE\Wow6432Node\Microsoft\VisualStudio\9.0\VC\VCRedist\x64';
  Result := not RegKeyExists(HKLM, key);
end;

function NeedsVC2015(): Boolean;
var
  key: String;
begin
  key := 'SOFTWARE\Microsoft\VisualStudio\14.0\VC\Runtimes\x64';
  Result := not RegKeyExists(HKLM, key);
end;

function NeedsSecuGenDriver(): Boolean;
var
  key: String;
begin
  key := 'SOFTWARE\SecuGen\SecuGen Driver';
  Result := not RegKeyExists(HKLM, key);
end;

procedure InitializeWizard();
begin
  ProgressPage := CreateOutputProgressPage('Comprobando dependencias', 
    'Por favor espere mientras el instalador verifica los componentes necesarios...');
  ProgressPage.Show;
  ProgressPage.SetProgress(0, 4);

  { Paso 1: VC++ 2008 }
  ProgressPage.SetText('Verificando Microsoft Visual C++ 2008 Redistributable...', '');
  Sleep(400);
  if NeedsVC2008() then
    ProgressPage.SetText('Visual C++ 2008 no encontrado. Se instalará.', '')
  else
    ProgressPage.SetText('Visual C++ 2008 ya está instalado.', '');
  ProgressPage.SetProgress(1, 4);

  { Paso 2: VC++ 2015–2022 }
  ProgressPage.SetText('Verificando Microsoft Visual C++ 2015–2022 Redistributable...', '');
  Sleep(400);
  if NeedsVC2015() then
    ProgressPage.SetText('Visual C++ 2015–2022 no encontrado. Se instalará.', '')
  else
    ProgressPage.SetText('Visual C++ 2015–2022 ya está instalado.', '');
  ProgressPage.SetProgress(2, 4);

  { Paso 3: Driver SecuGen }
  ProgressPage.SetText('Verificando controlador SecuGen...', '');
  Sleep(400);
  if NeedsSecuGenDriver() then
    ProgressPage.SetText('Driver SecuGen no encontrado. Se instalará.', '')
  else
    ProgressPage.SetText('Driver SecuGen ya está instalado.', '');
  ProgressPage.SetProgress(3, 4);

  { Paso 4: PATH }
  ProgressPage.SetText('Configurando variables del sistema...', '');
  Sleep(400);
  ProgressPage.SetProgress(4, 4);
  Sleep(300);

  ProgressPage.Hide;
end;
