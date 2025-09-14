# Simple Airport API - Fly.io Deployment

This directory contains the simple airport API server that gets automatically deployed to Fly.io.

## Automatic Deployment

The server is automatically deployed to Fly.io when:
- Changes are pushed to the `main` or `shared-state` branches
- Files in the `server/` directory are modified
- Manual deployment is triggered via GitHub Actions

## Setup Requirements

To enable automatic deployment, you need to:

1. **Create a Fly.io app**: 
   ```bash
   flyctl apps create simple-airport-api
   ```

2. **Add Fly.io API token to GitHub secrets**:
   - Get your Fly.io API token: `flyctl tokens create`
   - Add it as a secret named `FLY_API_TOKEN` in your GitHub repository settings

## Manual Deployment

You can also deploy manually:
```bash
cd server
flyctl deploy
```

## Environment Variables

- `PORT`: Set to 8080 for Fly.io deployment
- `NODE_ENV`: Automatically set to "production" in Docker

## API Endpoints

The server provides simple CRUD operations for airport data. See the main server code for endpoint details.
