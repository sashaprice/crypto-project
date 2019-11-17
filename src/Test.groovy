class Test {
    static void main(String[] args) {
        // Run this to test if your configuration works properly
        String imagePath = getBasePath() + "\\data\\input\\building-1.jpg"
        try {
            File file = new File(imagePath) // Verifies image is found
            println("File was successfully found")
        }
        catch (FileNotFoundException ignored) {
            println("Path-finding is broken")
        }
    }

    private static String getBasePath() {
        return new File("").getAbsolutePath()
    }
}