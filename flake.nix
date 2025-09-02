{
  description = "Clojure development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
    process-compose-flake.url = "github:Platonic-Systems/process-compose-flake";
  };

  outputs = inputs@{ self, nixpkgs, flake-parts, process-compose-flake, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" "x86_64-darwin" ];
      
      imports = [
        inputs.process-compose-flake.flakeModule
      ];

      perSystem = { config, self', inputs', pkgs, system, ... }: {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            clojure
            leiningen
            babashka
            # Java is required for Clojure
            jdk
            # Temporal.io CLI
            temporal-cli
            # PostgreSQL client tools
            postgresql_17
            # Temporal server
            temporal
          ];

          # Start services
          inputsFrom = [
            config.process-compose."dev".outputs.package
          ];

          shellHook = ''
            echo "Clojure development environment with services"
            echo "Clojure version: $(clojure --version)"
            echo "Leiningen version: $(lein --version)"
            echo "Babashka version: $(bb --version)"
            echo "Java version: $(java --version 2>&1 | head -n1)"
            echo "Temporal CLI version: $(temporal --version)"
            echo ""
            echo "To start services, run: nix run .#dev"
          '';
        };

        process-compose."dev" = {
          settings.processes = {
            postgres = {
              command = ''
                mkdir -p ./data/postgres
                export USER=postgres
                if [ ! -d "./data/postgres/base" ]; then
                  echo "Initializing PostgreSQL database..."
                  ${pkgs.postgresql_17}/bin/initdb -D ./data/postgres --username=postgres --auth=trust
                  echo "host all all 127.0.0.1/32 trust" >> ./data/postgres/pg_hba.conf
                  echo "host all all ::1/128 trust" >> ./data/postgres/pg_hba.conf
                fi
                ${pkgs.postgresql_17}/bin/postgres -D ./data/postgres -p 5432
              '';
            };

            postgres-init = {
              command = ''
                # Wait for PostgreSQL to be ready
                until ${pkgs.postgresql_17}/bin/pg_isready -h localhost -p 5432; do
                  echo "Waiting for PostgreSQL to be ready..."
                  sleep 2
                done
                
                
                # Create app database and admin user
                ${pkgs.postgresql_17}/bin/createdb -h localhost -p 5432 -U postgres app 2>/dev/null || true
                
                ${pkgs.postgresql_17}/bin/psql -h localhost -p 5432 -U postgres -tc "SELECT 1 FROM pg_user WHERE usename = 'admin'" | grep -q 1 || \
                ${pkgs.postgresql_17}/bin/psql -h localhost -p 5432 -U postgres -c "CREATE USER admin WITH SUPERUSER PASSWORD 'admin';"
                
                echo "PostgreSQL setup complete - databases and users created"
              '';
              depends_on = {
                postgres = {
                  condition = "process_started";
                };
              };
            };

            temporal = {
              command = ''
                mkdir -p ./data/temporal
                cd ./data/temporal
                ${pkgs.temporal-cli}/bin/temporal server start-dev \
                  --db-filename temporal.db \
                  --port 7233 \
                  --ui-port 8088
              '';
            };
          };
        };
      };
    };
}