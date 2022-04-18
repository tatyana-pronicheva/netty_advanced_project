import java.util.ArrayList;

public class AuthorizationService {
      private static Credentials[] usersList = {
                    new Credentials("login1","pass1"),
                    new Credentials("login2","pass2")
      };
      private static ArrayList<String> alreadyAuthorized = new ArrayList<>();

      public static boolean authorizationAttempt(String login, String password) {
          for (int i = 0; i < usersList.length; i++) {
              if (usersList[i].getLogin().equals(login)
                      && usersList[i].getPassword().equals(password)
                      && !alreadyAuthorized.contains(login)) {
                  alreadyAuthorized.add(login);
                  System.out.println("Успешная авторизация");
                  return true;
              }
          }
          System.out.println("Попытка авторизации провалилась. Неправильный логпасс или такой юзер уже зарегестрирован");
          return false;
      }
}

