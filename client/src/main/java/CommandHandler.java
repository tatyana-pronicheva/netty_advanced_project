import java.io.File;

public class CommandHandler {
    Client client;
    boolean commandIsRecognized;

    CommandHandler(Client client){
        this.client = client;
        System.out.println("  Для отображения списка воозможных команд введите 'help'");
        while (true) {
            String input = System.console().readLine();
            commandIsRecognized = false;
            handle(input);
        }
    }
    public void handle(String command){
        if ("q".equals(command)) {
            commandIsRecognized = true;
            System.out.println("  Exit!");
            System.exit(0);
        }
        if ("help".equals(command)){
            commandIsRecognized = true;
            printHelpMessage();
        };
        try{
        if (command.startsWith("push")){
            commandIsRecognized = true;
            String[] splittedCommand = command.split(" ");
            String fileSourcePoint = splittedCommand[1];
            String fileDestinationPoint = fileSourcePoint.substring(fileSourcePoint.lastIndexOf("/")+1);
            client.sendFile(fileSourcePoint, fileDestinationPoint);
        }
        if (command.startsWith("pull")){
            commandIsRecognized = true;
            String[] splittedCommand = command.split(" ");
            String fileSourcePoint = splittedCommand[1];
            String fileDestinationPoint = splittedCommand[2];
            if((new File(fileDestinationPoint)).isDirectory()){
                fileDestinationPoint = fileDestinationPoint + fileSourcePoint.substring(fileSourcePoint.lastIndexOf("/")+1);
                client.getFileFromServer(fileSourcePoint,fileDestinationPoint);
            } else {
                client.getFileFromServer(fileSourcePoint,fileDestinationPoint);

            }
        }

        if (command.startsWith("serverdelete")){
            commandIsRecognized = true;
            String[] splittedCommand = command.split(" ");
            String filePath = splittedCommand[1];
            client.deleteFromServer(filePath);
        }
        if ("filelist".equals(command)){
            commandIsRecognized = true;
            client.requestFilelist();
        }
        }catch (Exception e) {
            System.out.println("  Попробуйте ввести команду еще раз");
        }
        if (!commandIsRecognized) {
            System.out.println("  Команда не распознана. Пожалуйста, введите команду из списка:");
            printHelpMessage();}
    }

    private void printHelpMessage(){
        System.out.println("  q - выйти из приложения");
        System.out.println("  help - показать подсказку");
        System.out.println("  push {локальный путь} - скопировать файл на сервер");
        System.out.println("  pull {название файла} {локальный путь} - скопировать файл c сервера в папку");
        System.out.println("  serverdelete {название файла} - удалить файл с сервера");
        System.out.println("  filelist - посмотреть список файлов на сервере");
    }
}
