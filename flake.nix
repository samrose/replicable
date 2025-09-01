{
  description = "Clojure development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            clojure
            leiningen
            # Java is required for Clojure
            jdk
            # Temporal.io CLI and server
            temporal-cli
            temporal
            # PostgreSQL 17
            postgresql_17
          ];

          shellHook = ''
            echo "Clojure development environment with Temporal.io and PostgreSQL"
            echo "Clojure version: $(clojure --version)"
            echo "Leiningen version: $(lein --version)"
            echo "Java version: $(java --version 2>&1 | head -n1)"
            echo "Temporal CLI version: $(temporal --version)"
            echo "PostgreSQL version: $(postgres --version)"
          '';
        };
      });
}