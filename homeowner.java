import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.opencsv.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.Select;


public class homeowner {
    Map<String, String> prefs = new HashMap<String, String>(); //Holds preferences.
    ArrayList<String> zipCodes;
    ArrayList<String[]> data = new ArrayList<String[]>();

    int validZips = 0;
    String DROPDOWN_DEFAULT = "Other";
    String STREET_DEFAULT = "1";
    String progressString = "";

    public homeowner(){
        this.loadPreferences();
        this.loadZips();
    }

    public void loadPreferences(){
        BufferedReader reader;
        try {
            Scanner scan = new Scanner(new File("preferences.txt"));
            while (scan.hasNext()){
                String line = scan.nextLine();
                this.prefs.put(line.split("=")[0],
                        line.split("=")[1]);
            }
//            System.out.println(this.map.toString());
            scan.close();
        }
        catch(FileNotFoundException f){
            System.out.println("preferences.txt not found.");
            System.exit(1);
        }
    }
    public void buildSpreadsheet() {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter("Annual_Premiums.csv"));
            for (String[] record: this.data)
                writer.writeNext(record);
        }catch(IOException e){
            System.out.println("There was an issue with the FileWriter.");
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Data written to spreadsheet.");
    }
    public void updateProgress(){
        this.progressString = "" + ((this.validZips/this.zipCodes.size())*100) + "% out of "
                + this.zipCodes.size() + " codes checked.";
    }
    public String getDescription(){
        return prefs.toString();
    }
    public void loadZips(){
        String csvPath = "zip_database.csv";
        zipCodes = new ArrayList<String>();
        int zipCount = 0;
        int lineCount = 0;

        try {
            Scanner scan = new Scanner(new File(csvPath));
            while(scan.hasNext()){
                String[] line = scan.nextLine().split(",");

                if (!(lineCount == 0)) {
                    if (!line[1].equals("\"PO BOX\"") && line[3].equals("\"CA\"") && line[8].equals("\"false\"")) {
                        this.zipCodes.add(line[0]);
                        zipCount++;
                    }
                }
                lineCount++;
            }
            scan.close();
//            System.out.println(this.zipCodes.toString());
//            System.out.printf("Number of Zips: %s.\n",zipCount);
        } catch (FileNotFoundException e) {
            System.out.printf("File at %s not found\n", csvPath);
            System.exit(1);
        }
    }
    public void startToState(WebDriver driver, final String DROPDOWN_DEFAULT, final String STREET_DEFAULT){
        WebElement startDate = driver.findElement(By.id("startdate"));
        startDate.clear();
        startDate.click();
        startDate.sendKeys(this.prefs.get("Policy_Start_Date"));

        WebElement insuranceDropdown = driver.findElement(By.id("participatingInsurer"));
        insuranceDropdown.click();
        Select insuranceDropOpt =  new Select(driver.findElement(By.id("participatingInsurer")));
        insuranceDropOpt.selectByVisibleText(DROPDOWN_DEFAULT);

        WebElement street = driver.findElement(By.id("street"));
        street.clear();
        street.sendKeys(STREET_DEFAULT);

        WebElement state = driver.findElement(By.id("state"));
        state.clear();
        state.sendKeys(this.prefs.get("State"));
    }
    public void enterZip(WebDriver driver, String zip){
        WebElement zipTextBox = driver.findElement(By.id("zipcode"));
        zipTextBox.clear();
        zipTextBox.sendKeys(zip);

        WebElement getStarted = driver.findElement(By.cssSelector("button.btn.btn-block.btn-primary.ng-binding.ng-scope"));
        getStarted.click();

        WebElement yearBuiltBox = driver.findElement(By.id("yearbuilt"));
        yearBuiltBox.clear();
        yearBuiltBox.sendKeys(this.prefs.get("Year_Built"));
    }

    public void getAnnualPremiums(String zip){
        System.setProperty("webdriver.chrome.driver", this.prefs.get("Path"));
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        System.setProperty("webdriver.chrome.silentOutput", "true");
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.OFF);
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        try{
            driver.get("https://calc.earthquakeauthority.com/app/");

            //Homeowner coverage button
            driver.findElement(By.cssSelector("button.btn.cea-policy-btn.ng-binding")).click();

            this.startToState(driver, this.DROPDOWN_DEFAULT, this.STREET_DEFAULT);
            this.enterZip(driver, zip);
            WebElement insuredValBox = driver.findElement(By.id("insuredvalue"));
            insuredValBox.clear();
            insuredValBox.sendKeys(this.prefs.get("Insured_Value"));

            Select stories = new Select(driver.findElement(By.id("numberofstories")));
            stories.selectByVisibleText("Greater than one");

            Select foundation = new Select(driver.findElement(By.id("foundationtype")));
            foundation.selectByVisibleText(this.DROPDOWN_DEFAULT);

            Select roof = new Select(driver.findElement(By.id("rooftype")));
            roof.selectByVisibleText(this.DROPDOWN_DEFAULT);

            WebElement getEstimate = driver.findElement(By.cssSelector("button.btn.btn-block.btn-primary." +
                    "single-button.ng-binding"));
            getEstimate.click();

            Thread.sleep(1000);
            String annualPrem = driver.findElement(By.cssSelector("div.gauge-subtitle.ng-binding.ng-scope")).getText()
                    .replace("Annual Premium: ", "")
                    .replace("$", "");

            double premium = Double.parseDouble(annualPrem);
            System.out.println(premium);
//            this.data.put(Integer.parseInt(zip), premium);
            String prepString = Integer.parseInt(zip) + "," + premium + "";
            String[] input = prepString.split(",");
            this.data.add(input);
            validZips++;


        }catch(WebDriverException e){
            System.out.println("A WebDriver issue occured.");
            updateProgress();
        }catch (NoSuchElementException e){
            System.out.println("An element could not be found.");
            updateProgress();
        }catch (InterruptedException e){
            System.out.println("A thread was interrupted.");
            updateProgress();
        }
        finally {
            driver.manage().deleteAllCookies();
            driver.quit();
        }
    }
}
