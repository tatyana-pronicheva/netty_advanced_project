public class AuthorizationService {
      private static Credentials[] usersList = {
                    new Credentials("login1","pass1"),
                    new Credentials("login2","pass2")
      };
      public static boolean authorizationAttempt(String login, String password) {
          for (int i = 0; i < usersList.length; i++) {
              if (usersList[i].getLogin().equals(login) && usersList[i].getPassword().equals(password)) {
                  System.out.println("Успешная авторизация");
                  return true;
              }
          }
          System.out.println("Неправильный логин или пароль");
          return false;
      }
}

