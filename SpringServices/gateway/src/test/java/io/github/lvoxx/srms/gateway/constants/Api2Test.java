package io.github.lvoxx.srms.gateway.constants;

public enum Api2Test {
    // Main paths
    CUSTOMERS("/customers"),
    CONTACTORS("/contactors");

    private final String path;

    Api2Test(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    // Combine main path + subpath
    public String with(SubPath subPath) {
        return this.path + subPath.getPath();
    }

    // Subpath definitions
    public enum SubPath {
        TEST("/test"),
        DETAILS("/details"),
        CREATE("/create");

        private final String path;

        SubPath(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }
}
