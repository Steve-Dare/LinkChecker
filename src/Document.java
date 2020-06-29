import java.util.ArrayList;

public class Document {

    private String slug;
    private String category;
    private ArrayList<String> titles;

    public Document() {
        titles=new ArrayList<String>();
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void addTitle(String title) {
        titles.add(title);
    }

    public void display() {
        System.out.println("Document: " + category + "/" + slug);
        System.out.println();
        titles.forEach((s) -> System.out.println(s));
        System.out.println();
    }

    public boolean validId(final String requiredCategory, final String requiredSlug, final String requiredTitle) {

//        System.out.println("VALIDID - " + "," + requiredCategory + "," + requiredSlug + "," + requiredTitle);

        if (!requiredCategory.equals(this.category)) {
            return false;
        }
        if (!requiredSlug.equals(this.slug)) {
            return false;
        }
        if (requiredTitle!=null && !requiredTitle.isEmpty()) {
            boolean found = false;
            for (String title : titles) {
                if (requiredTitle.equals(title)) {
                    found = true;
                }
            }
            return found;
        }
        return true;
    }

}

