# AMAN/DMAN API Server

API server used for MASTER <-> SLAVE communication.

## Prerequisites

- Node.js 18 or higher
- npm

## Development

### Install Dependencies

```bash
npm install
```

### Run in Development Mode

Development mode uses `ts-node` to run TypeScript directly without compilation:

```bash
npm run dev
```

### Build for Production

Compile TypeScript to JavaScript:

```bash
npm run build
```

This will create compiled JavaScript files in the `dist/` directory.

### Run in Production Mode

After building, start the server with:

```bash
npm start
```

## Docker

Build the Docker image:

```bash
docker build -t simple-airport-api .
```

Run the container:

```bash
docker run -p 8080:8080 simple-airport-api
```

## Environment Variables

- `PORT` - Server port (default: 3000)
- `GITHUB_TOKEN` - GitHub API token for fetching release information
- `NODE_ENV` - Environment mode (production/development)
