const { spawn } = require('child_process');
const port = process.env.PORT || 4200;

const child = spawn(
    'npm',
    ['run', 'ng', '--', 'serve', '--configuration=fr', `--port=${port}`, '--open'],
    { stdio: 'inherit' }
);

child.on('exit', code => process.exit(code));
