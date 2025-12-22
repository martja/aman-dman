# AMAN/DMAN API Server

API server used for MASTER/SLAVE communication.

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

### Run Tests

```bash
npm test
```

### Code Quality

This project uses ESLint and Prettier for code quality and formatting:

```bash
# Check for linting errors
npm run lint

# Auto-fix linting errors
npm run lint:fix

# Format all files
npm run format

# Check formatting without modifying
npm run format:check
```

**Automatic on Commit**: When you commit files, a pre-commit hook automatically runs ESLint and Prettier on staged files. See [CODE-QUALITY.md](CODE-QUALITY.md) for details.

## Project Structure

```
server/
├── src/
│   ├── server.ts    # Main server application
│   └── test-overwrite.ts   # Test script
├── dist/                   # Compiled JavaScript (generated)
├── .husky/                # Git hooks
├── Dockerfile             # Docker configuration
├── package.json          # Dependencies and scripts
├── tsconfig.json        # TypeScript configuration
├── .eslintrc.json       # ESLint configuration
├── .prettierrc          # Prettier configuration
└── .gitignore          # Git ignore rules
```

## API Endpoints

See the console output when the server starts for a complete list of endpoints.

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
