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

  // HTTP Basic Auth helpers – builds a 'Basic <base64>' header for the given role profile.
  // Reads credentials from API_CREDENTIALS_JSON env var; falls back to safe local-dev defaults.
  const DEFAULT_CREDS = '{"admin":{"user":"api_admin","pass":"admin123","permissions":7},' +
      '"writer":{"user":"api_writer","pass":"writer123","permissions":6},' +
      '"reader":{"user":"api_reader","pass":"reader123","permissions":4}}';

  const credsJson = java.lang.System.getenv('API_CREDENTIALS_JSON') || DEFAULT_CREDS;
  const creds = JSON.parse(credsJson);

  function authHeader(role) {
    const c = creds[role];
    const combined = c.user + ':' + c.pass;
    const bytes = new java.lang.String(combined).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    const token = java.util.Base64.getEncoder().encodeToString(bytes);
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