# Update your existing GitHub + Render deployment

This guide assumes the existing Render service is connected to a GitHub repository. Keep the same repository and Render service if you want to keep the existing public URL.

1. Download and extract `dispatch-load-balancer-final.zip` on your computer.
2. Open the GitHub repository that Render currently deploys.
3. Choose **Code → Codespaces** and open/create a Codespace for that repository.
4. In the Codespaces Explorer, upload `dispatch-load-balancer-final.zip` into the repository root (drag it from your computer's Downloads folder into the Explorer).
5. Open the Codespaces terminal and run:

   ```bash
   unzip -o dispatch-load-balancer-final.zip
   cp -a dispatch-load-balancer-final/. .
   git add .
   git commit -m "Upgrade dispatch load balancer"
   git push
   ```

6. Open the existing Render service. It should detect the new commit and start a Docker deployment. Wait until the deployment is live and `/health` succeeds.
7. Open your existing Swagger URL and run the requests in this order: `DELETE /api/dispatch/data`, `POST /api/dispatch/vehicles`, `POST /api/dispatch/orders`, `GET /api/dispatch/plan`.
8. Use the new Postman collection at `postman/Dispatch-Load-Balancer.postman_collection.json` for the same test.

If Render is not connected to the repository, connect it under the service's Settings → Build & Deploy area, or create a new Docker Web Service from the repository. A new service receives a new public URL.

**Important:** the updated application defaults to a local H2 file database. Render's default filesystem may be ephemeral, so re-submit your vehicles and orders after redeployment/restart unless you configure a persistent disk or PostgreSQL. The API's public URL does not change when the existing Render service is redeployed.
