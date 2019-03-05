;This file will be executed next to the application bundle image
;I.e. current directory will contain folder APPLICATION_NAME with application files
[Setup]
AppId={{PRODUCT_APP_IDENTIFIER}}
AppName=APPLICATION_NAME
AppVersion=APPLICATION_VERSION
AppVerName=APPLICATION_NAME APPLICATION_VERSION
AppPublisher=APPLICATION_VENDOR
AppComments=APPLICATION_COMMENTS
AppCopyright=APPLICATION_COPYRIGHT
;AppPublisherURL=http://java.com/
;AppSupportURL=http://java.com/
;AppUpdatesURL=http://java.com/
DefaultDirName=APPLICATION_INSTALL_ROOT\APPLICATION_NAME
DefaultGroupName=APPLICATION_GROUP
;Optional License
LicenseFile=APPLICATION_LICENSE_FILE
;WinXP or above
MinVersion=0,5.1
OutputBaseFilename=INSTALLER_FILE_NAME
Compression=lzma
SolidCompression=yes
PrivilegesRequired=APPLICATION_INSTALL_PRIVILEGE
SetupIconFile=APPLICATION_NAME\APPLICATION_NAME.ico
UninstallDisplayIcon={app}\APPLICATION_NAME.ico
UninstallDisplayName=APPLICATION_NAME
WizardImageStretch=No
WizardSmallImageFile=APPLICATION_NAME-setup-icon.bmp
ArchitecturesInstallIn64BitMode=ARCHITECTURE_BIT_MODE
FILE_ASSOCIATIONS

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "APPLICATION_NAME\APPLICATION_NAME.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "APPLICATION_NAME\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\APPLICATION_NAME"; Filename: "{app}\APPLICATION_NAME.exe"; IconFilename: "{app}\APPLICATION_NAME.ico"; Check: APPLICATION_MENU_SHORTCUT()
Name: "{commondesktop}\APPLICATION_NAME"; Filename: "{app}\APPLICATION_NAME.exe";  IconFilename: "{app}\APPLICATION_NAME.ico"; Check: APPLICATION_DESKTOP_SHORTCUT()
SECONDARY_LAUNCHERS

[Run]
Filename: "{app}\RUN_FILENAME.exe"; Parameters: "-Xappcds:generatecache"; Check: APPLICATION_APP_CDS_INSTALL()
Filename: "{app}\RUN_FILENAME.exe"; Description: "{cm:LaunchProgram,APPLICATION_NAME}"; Flags: nowait postinstall skipifsilent; Check: APPLICATION_NOT_SERVICE()

[Code]
function returnTrue(): Boolean;
begin
  Result := True;
end;

function returnFalse(): Boolean;
begin
  Result := False;
end;

function InitializeSetup(): Boolean;
begin
  Result := True;
end;

procedure CurUninstallStepChanged (CurUninstallStep: TUninstallStep);
var
     mres : integer;
begin
    case CurUninstallStep of usPostUninstall:
        begin
          mres := MsgBox('Do you want to remove user data (this includes your wallet)?', mbConfirmation, MB_YESNO or MB_DEFBUTTON2)
          if mres = IDYES then
            DelTree(ExpandConstant('{app}'), True, True, True);
        end;
    end;
end; 