# Hermanas Client

This project is a web application dedicated to monitor and take control remotely of your chicken coop.
This automation solution is currently operating from my backyard. You may try it on http://www.hermanas.fr.

The back-end application, running inside the chicken coop, is hosted in a separate GitHub project on https://github.com/jibe77/hermanas.

## Architecture

This application is based on Angular 11.
Hosting is made on Amazon cloud AWS.
It is communicating with your chicken coop using Rest services.

How to fetch the project :

```bash
git clone git@github.com:startbootstrap/hermanas-client.git
cd hermanas-client
npm install
npm start
```

`npm start` should open a browser window to <http://localhost:4200>

By default angular runs on port 4200. To change this port you can run:

```bash
# This starts the development server on port 4205,
# but you can use any port you'd like
export PORT=4205 && npm start
```

## Tests

### Unit Tests

```bash
npm run test
```

### e2e

```bash
npm run e2e
```

## Production

This application come with a production ready Dockerfile and build scripts.

You can get Docker [here](https://www.docker.com/get-started)

```bash
npm run docker:build
npm run docker:run
```

## Generate Code

```bash
npm run generate:module -- --path src/modules --name Test
npm run generate:component -- --path src/modules/test/containers --name Test
npm run generate:component -- --path src/modules/test/components --name Test
npm run generate:directive -- --path src/modules/test/directives --name Test
npm run generate:service -- --path src/modules/test/services --name Test
```

_Note: Creating a Component and a Container use the same command,
the difference is just the paths and how they are used._

### MVCC

Containers and Components are both Angular Components, but used in different ways.

Containers should arrange Components.

Obviously this can become subjective, but MVCC is the paradigm that we subscribe to.

## Troubleshooting

### npm start

If you receive memory issues adjust
`max_old_space_size` in the `ng` command of the `package.json`:

```json
"ng": "cross-env NODE_OPTIONS=--max_old_space_size=2048 ./node_modules/.bin/ng",
```

You can adjust 2048 to any number you need.

Keep in mind that this project only uses node to build the angular application.
There is no production dependency on node.
