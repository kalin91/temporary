function fn() {
  let env = karate.env; // Captura la variable de sistema 'karate.env'
  karate.log('Karate env system property was:', env);
  
  if (!env) {
    env = 'local'; // Entorno por defecto
  }

  const config = {
    baseUrl: 'http://localhost:8080',
    basePath: '/model',
    myToken: 'default-token'
  };

  if (env == 'local') {
    config.baseUrl = 'http://localhost:' + (karate.properties['demo.server.port'] || '8080');
  } else if (env == 'prod') {
    config.baseUrl = 'https://pending.com';
  }

  // HTTP Basic Auth – admin credentials are used because the test suite covers mutations.
  // These are demo credentials defined in application.yml.
  const json = java.lang.System.getenv('API_CREDENTIALS_JSON');

  if (!json) {
    karate.log('WARNING: API_CREDENTIALS_JSON not set');
  }

  const creds = karate.fromJson(json);

  function authHeader(role) {
    const user = creds[role].user;
    const pass = creds[role].pass;

    const token = java.util.Base64.getEncoder()
      .encodeToString((user + ':' + pass).bytes);

    return 'Basic ' + token;
  }

  karate.set('authHeader', authHeader);

  // Tiempo de espera para peticiones (en milisegundos)
  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 5000);
  
  // Imprimir siempre requests y responses para depuración
  karate.configure('logPrettyRequest', true);
  karate.configure('logPrettyResponse', true);

  return config;
}