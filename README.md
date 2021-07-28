(Latest update of this document: Jul 28 2021)  

# Proteomics Cell Type Enrichment Analysis (PCTSEA)

This tool has been designed to be a cell type enrichment tool for proteomics quantitative data.   
It compares publicly available RNAseq single cell datasets with the input protein list and performs an enrichment analysis based on Kolmogorov-Smirnov statistics to end up determining the set of cell types that are significantly enriched in the input data.   
   
### Availability of the software:
PCTSEA server can be found at http://pctsea.scripps.edu   
PCTSEA is also available as a command line at http://sealion.scripps.edu/pCtSEA/  
Source code of PCTSEA can be found in this GitHub repository: https://github.com/proteomicsyates/pctsea-parent   
 - [pctsea-core](https://github.com/proteomicsyates/pctsea-parent/tree/main/pctsea-core): source code of the core of its functionality
 - [pctsea-cl](https://github.com/proteomicsyates/pctsea-parent/tree/main/pctsea-cl): command-line version of the tool
 - [pctseaweb](https://github.com/proteomicsyates/pctsea-parent/tree/main/pctseaweb): web version of the tool  
 - [pctsea-results-viewer](https://github.com/proteomicsyates/pctsea-parent/tree/main/pctseaweb/shinyR): Shiny app for visualization of results.
 - [pctsea-results-comparator](https://github.com/proteomicsyates/pctsea-parent/tree/main/pctseaweb/shinyR_comparison): Shiny app for comparison of results.
   
   
### BioRXiv publication:
https://doi.org/10.1101/2021.02.16.431318   
(Posted February 16, 2021)



## Implementation description:
**pctsea-cl** and **pctseaweb** modules are build with SpringBoot framework and pctseaweb with [Vaadin](https://vaadin.com/).  
There is a database storing the single cell expression values, implemented in [MongoDB](https://www.mongodb.com/).   

## pctsea-cl, Command line version:  
It is implemented with StringBoot framework. The class that contains the main method is PCTSEADbApplication.java that implements CommandLineRunner (SpringBoot).  
Command line parameters are defined constants variables at InputParameters.java and used at PCTSEACommandLine.java where they are defined (*defineCommanLineOptions* method) and read (*initToolFromComamndLineOptions* method) so that they are passed to the *PCTSEA* object at *run* method. 

Example of parameters:  
`
-perm 50 -eef Z:\share\Salva\data\cbamberg\mouse_GRIA_IP\mouse_GRIA_IP_gt10SPC.txt -min_score 0 -email salvador@scripps.edu -out pearson_ms0_mgc4_mc02_Bal -min_genes_cells 4 -min_corr 0.2 -datasets HCL -scoring_method PEARSONS_CORRELATION -input_data_type IP -create_zip true -write_scores --spring.data.mongodb.port=27017 --spring.data.mongodb.host=sealion.scripps.edu
`  
Note that parameters `--spring.data.mongodb.port=27017 --spring.data.mongodb.host=sealion.scripps.edu` are used to determine the connection to the MongoDB database. However, if these parameters are not provided, it will try port *27017* and host: *locahost* by default, which might be enough if you are running it in the same machine than the MongoDB is located.

Using the command line version in an standalone GUI:  
If you run the program with ***-gui*** parameter as:  
`
-gui  --spring.data.mongodb.port=27017 --spring.data.mongodb.host=sealion.scripps.edu
`  
a Java-based interfaze is **automatically built** using the parameters defined. If new parameters are defined in *InputParameters* and *CommandLineRunner*, a new input text, or checkbox will be automatically created in this interfaze without the need of doing anything else:  
![Command line GUI](https://github.com/proteomicsyates/pctsea-parent/raw/main/docs/pctsea-cli-gui.png)



## pctseaweb, Web Application version:
It is implemented with SpringBoot + Vaadin frameworks. The class with the main method is called *Application* and extends from *SpringBootServletInitializer* (SpringBoot).  
The different tab menus in the web application are defined as **views** and are located under *edu.scripps.yates.pctsea.views* package. In order to incorporate more views just implement a new class as this one:
```java
@Route(value = "about", layout = MainView.class)
@PageTitle("About")
@CssImport("./styles/views/about/about-view.css")
public class AboutView extends Div {
	@Autowired
	private PctseaRunLogRepository runRepo;

	public AboutView() {
	}

	@PostConstruct
	public void init() {
		setId("about-view");
		add(new Label("Proteomics Cell Type Set Enrichment Analysis (PCTSEA)"));
   }
}
```
And then, include it in the main view at **MainView.java** as:
```java
private static Tab[] getAvailableTabs() {
   return new Tab[] { 
      createTab("Home", HomeView.class), 
      createTab("Analyze", AnalyzeView.class),
	   createTab("Compare results", CompareResultsView.class), 
      createTab("About", AboutView.class) };
}
```
The "Analysis" page with the form to input the parameters and input file for analysis is built in *AnalyzeView.java* class. In this class, an object from class *InputParameters.java* is build when the user clicks on submit button. Some parameters of this object are automatically populated via Vaadin bind feature but others are set programatically at the *submit* method of *AnalyzeView.java* class. Once the *InputParameters* object is built, is passed to method *startPCTSEAAnalysis* to build the *PCTSEA* object and run the analysis.  

## FLow chart:   
![Flow chart](https://github.com/proteomicsyates/pctsea-parent/raw/main/docs/flow_chart.png)

Both command line and web version are coupled to the pctsea-core module where the ***PCTSEA.java*** class is defined and where the logic of the analysis is implemented.  
For a more detailed description of how the analysis is implemented in this class go to: (PCTSEA for developers)[]

