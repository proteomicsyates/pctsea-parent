#
# This is a Shiny web application. You can run the application by clicking
# the 'Run App' button above.
#
# Find out more about building applications with Shiny here:
#
#    http://shiny.rstudio.com/
#
list.of.packages <- c("shiny","dplyr", "stringr", "ggplot2",
                      "sjmisc", "tidyverse", "data.table",
                      "promises", "future", "tools", "stringi",
                      "filesstrings", "DT", "plotly")
new.packages <- list.of.packages[!(list.of.packages %in% installed.packages()[,"Package"])]
if(length(new.packages)){
  # install.packages(new.packages)
}
library(shiny)
library(dplyr)
library(stringr)
library(ggplot2)
library(sjmisc)
library(tidyverse)
library(data.table)
library(promises)
library(future)
library(tools)
library(stringi)
library(filesstrings)
library(DT)
library(plotly)
# plan(multisession)
plan(multicore)

options(shiny.maxRequestSize = 120*1024^2,
        shiny.reactlog = TRUE
        # ,shiny.trace = TRUE
        ,shiny.fullstacktrace = TRUE
        ,launch.browser = TRUE
)

# load("./data/alldata.Rdata")

# Define UI for application that draws a histogram
ui <- fluidPage(title = "PCTSEA",
                fluidRow(
                  column(12, uiOutput(outputId = "titleUI"))
                ),
                # sidebarLayout(
                #   sidebarPanel(width = 2,
                #
                #                conditionalPanel(
                #                  condition = "input.tabs == 'Enrichment Table'",
                #                  p("Here you have the main output table")
                #                ),
                #
                #   ),
                fluidRow(
                  column(width = 12,
                         tabsetPanel(id = "tabs",
                                     tabPanel("Import data", icon = icon("file-import"),
                                              br(),

                                              uiOutput(outputId = "importSideControlUI"),

                                              br(),
                                              uiOutput(outputId = "importControlUI"),
                                     ),
                                     tabPanel("Input parameters", icon = icon("ellipsis-v"),
                                              br(),
                                              h4("Input parameters:"),
                                              verbatimTextOutput(outputId = "inputParametersText")
                                     ),
                                     tabPanel("Enrichment Table", icon = icon("table"),
                                              br(),
                                              p("Here you have the main output table"),
                                              fluidRow(
                                                column(width = 12,
                                                       DT::dataTableOutput(outputId = "enrichmentDataTable"), style = "font-size:80%; rowHeight: 75%"
                                                )
                                              )
                                     ),
                                     tabPanel("Global charts",icon = icon("bar-chart-o"),
                                              br(),
                                              fluidRow(
                                                column(4,
                                                       wellPanel(
                                                         style = "background: white",
                                                         fluidRow(
                                                           column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'globalCorrelationsPlotHelp', label = '', icon = icon('question-circle'))))
                                                         ),
                                                         plotlyOutput(outputId = "globalCorrelationsPlot", height = "300px")
                                                       )
                                                ),

                                                column(4,
                                                       wellPanel(
                                                         style = "background: white",
                                                         fluidRow(
                                                           column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'globalCorrelationsRankPlotHelp', label = '', icon = icon('question-circle'))))
                                                         ),
                                                         plotlyOutput(outputId = "globalCorrelationsRankPlot", height = "300px")
                                                       )
                                                ),
                                                column(4,
                                                       wellPanel(
                                                         style = "background: white",
                                                         fluidRow(
                                                           column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'globalGenesPerCellTypePlotHelp', label = '', icon = icon('question-circle'))))
                                                         ),
                                                         plotlyOutput(outputId = "globalGenesPerCellTypePlot", height = "300px")
                                                       )
                                                )
                                              ),

                                              fluidRow(
                                                column(4,
                                                       wellPanel(
                                                         style = "background: white",
                                                         fluidRow(
                                                           column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'globalMultipleTestingCorrectionPlotHelp', label = '', icon = icon('question-circle'))))
                                                         ),
                                                         plotlyOutput(outputId = "globalMultipleTestingCorrectionPlot", height = "300px")
                                                       )
                                                ),
                                                column(4,
                                                       wellPanel(
                                                         style = "background: white",
                                                         fluidRow(
                                                           column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'globalSupremaHistogramPlotHelp', label = '', icon = icon('question-circle'))))
                                                         ),
                                                         plotlyOutput(outputId = "globalSupremaHistogramPlot", height = "300px")
                                                       )
                                                ),
                                                column(4,
                                                       wellPanel(
                                                         style = "background: white",
                                                         fluidRow(
                                                           column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'globalSupremaScatterPlotHelp', label = '', icon = icon('question-circle'))))
                                                         ),
                                                         plotlyOutput(outputId = "globalSupremaScatterPlot", height = "300px")
                                                       )
                                                )
                                              )
                                     ),
                                     tabPanel("Charts per cell type", icon = icon("bar-chart-o"),
                                              br(),
                                              fluidRow(style='padding-left:10px;',
                                                       h4("Select a cell type from the drop down menu or the table")
                                              ),
                                              fluidRow(
                                                column(4,
                                                       wellPanel(
                                                         style = "background: white",
                                                         fluidRow(
                                                           column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'cellTypeCorrelationsPlotHelp', label = '', icon = icon('question-circle'))))
                                                         ),
                                                         plotlyOutput(outputId = "cellTypeCorrelationsPlot", height = "300px")
                                                       )
                                                ),
                                                column(4,
                                                       wellPanel(
                                                         style = "background: white",
                                                         fluidRow(
                                                           column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'cellTypeScoreCalculationPlotHelp', label = '', icon = icon('question-circle'))))
                                                         ),
                                                         plotlyOutput(outputId = "cellTypeScoreCalculationPlot", height = "300px")
                                                       )
                                                ),
                                                column(4,
                                                       wellPanel(
                                                         style = "background: white",
                                                         fluidRow(
                                                           column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'genesPerCellTypePlotHelp', label = '', icon = icon('question-circle'))))
                                                         ),
                                                         plotlyOutput(outputId = "genesPerCellTypePlot", height = "300px")
                                                       )
                                                )
                                              ),
                                              fluidRow(
                                                column(4, selectInput(inputId = "selectCellType", label = "Select cell type", choices = c()))

                                              ),
                                              fluidRow(
                                                column(width = 6,
                                                       div(DT::dataTableOutput(outputId = "enrichmentDataTable2"), style = "font-size:80%; rowHeight: 75%")
                                                )
                                              )
                                     ),
                                     tabPanel("Clustering",icon = icon("project-diagram"),
                                              br(),
                                              # splitLayout(
                                              #   cellWidths = c("30%", "70%"),

                                              fluidRow(
                                                column(4, verticalLayout(
                                                  checkboxInput(inputId = 'showLabels', label = 'Show cell types labels', value = FALSE),
                                                  selectInput(inputId = "umapPlotDimensions", label = "Number UMAP components", choices = c("2D","3D","4D"), selected = "2D", width = "200px"),
                                                  div(DT::dataTableOutput(outputId = "enrichmentDataTableForCluster"), style = "font-size:70%; rowHeight: 65%")
                                                )

                                                ),
                                                column(8,
                                                       verticalLayout(
                                                         fluidRow(
                                                           column(4,
                                                                  wellPanel(
                                                                    style = "background: white;padding:2px",
                                                                    fluidRow(
                                                                      column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'umapAllPlotHelp', label = '', icon = icon('question-circle'))))
                                                                    ),
                                                                    plotlyOutput(outputId = "umapAllPlot", height = "250px")
                                                                  )
                                                           ),
                                                           column(4,
                                                                  wellPanel(
                                                                    style = "background: white;padding:2px",
                                                                    fluidRow(
                                                                      column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'umapHypGPlotHelp', label = '', icon = icon('question-circle'))))
                                                                    ),
                                                                    plotlyOutput(outputId = "umapHypGPlot", height = "250px")
                                                                  )
                                                           ),
                                                           column(4,
                                                                  wellPanel(
                                                                    style = "background: white;padding:2px",
                                                                    fluidRow(
                                                                      column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'umapKSTestPlotHelp', label = '', icon = icon('question-circle'))))
                                                                    ),
                                                                    plotlyOutput(outputId = "umapKSTestPlot", height = "250px")
                                                                  )
                                                           )
                                                         ),
                                                         fluidRow(
                                                           column(4,
                                                                  wellPanel(
                                                                    style = "background: white;padding:2px",
                                                                    fluidRow(
                                                                      column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'umapSig001PlotHelp', label = '', icon = icon('question-circle'))))
                                                                    ),
                                                                    plotlyOutput(outputId = "umapSig001Plot", height = "250px")
                                                                  )
                                                           ),
                                                           column(4,
                                                                  wellPanel(
                                                                    style = "background: white;padding:2px",
                                                                    fluidRow(
                                                                      column(12, align = 'right', tags$div(title='help about this chart', actionButton(inputId = 'umapSig005PlotHelp', label = '', icon = icon('question-circle'))))
                                                                    ),
                                                                    plotlyOutput(outputId = "umapSig005Plot", height = "250px")
                                                                  )
                                                           )
                                                         )
                                                       )
                                                )
                                              )
                                     ),
                                     tabPanel("Go to PCTSEA Analysis",icon = icon("cogs"),
                                              fluidRow(
                                                br(),
                                                br(),
                                                column(12, uiOutput("analysis_url"))
                                              )
                                     )
                         )
                  )
                )
)

# Define server logic required to draw a histogram
server <- function(input, output, session) {
  source("./functions.R", local=TRUE)
  observe({
    query <-parseQueryString(session$clientData$url_search)
    if (!is.null(query$results)) {
      inputFileName <- paste0(query$results,'.zip')
      tmp <- tools::file_path_sans_ext(basename(inputFileName))
      rv$run_name <- sub("[^_]+_[^_]+_(.+)", "\\1", tmp) # extract every after the second '_' until the end
      # str_extract(tmp, '[^_]+$')

      # input file should be in data folder
      zipfilepath = paste('data/', inputFileName, sep = "")

      # check if the file exist
      if (!file.exists(zipfilepath)){
        output$importSideControlUI <- renderUI({
          tagList(
            fluidRow(
              column(width = 12, h4("Opps!"))
            )
          )
        })
        output$importControlUI <- renderUI({
          tagList(
            fluidRow(
              column(width = 12, h4("Sorry, your analysis is not found on the server."))
            ),
            fluidRow(
              column(width = 12, h5("Please make sure the URL is correct or contact your administrator."))
            )
          )
        })
        return()
      }
      # title
      output$titleUI <- renderUI({
        h3("pCtSEA results explorer for run:", tags$b(rv$run_name))
      })

      # side panel
      output$importSideControlUI <- renderUI({
        tagList(
          fluidRow(
            column(4,wellPanel(

              h5("Analysis from ptcSEA run:", tags$b(rv$run_name))))
          )
        )
      })
      url <- paste(session$clientData$url_protocol, "//", session$clientData$url_hostname, ":", session$clientData$url_port, session$clientData$url_pathname, sep = "")
      output$importControlUI <- renderUI({
        tagList(
          p("Your dataset is imported in the pCtSEA results viewer."),

          p("You can also download your Zip file with your results here:",
            downloadButton(outputId = 'downloadData', label = "Download results Zip")),
          p("You could come back anytime here ", a(url, href = url), " and import the zip file to explore the results again."),
          br(),
          fluidRow(
            column(12,
                   h4("Explore the other tabs to see your data.")
            ),
            align = 'center'
          )
        )
      })

      # create download data button
      output$downloadData <- downloadHandler(
        filename = inputFileName,
        content = function(file){
          file.copy(zipfilepath, file, overwrite = TRUE)
        }
      )
      # unzip if not already unziped
      data_folder <- dirname(zipfilepath)
      folderTo <- paste(data_folder, "/", tools::file_path_sans_ext(basename(zipfilepath)), sep = "")
      if (!file.exists(folderTo)){
        withProgress({
          setProgress(message = "Unzipping results...", value = 0)
          unzip(zipfilepath, exdir = data_folder)
          setProgress(message = "Unzipping results...", value = 0.5)
          rv$unziped_files <- folderTo
          rv$global_correlations_file <- get_global_file(rv$unziped_files, rv$run_name, "corr_hist")
          rv$global_correlations_rank_file <- get_global_file(rv$unziped_files, rv$run_name, "corr_rank_dist")
          rv$global_genes_file <- get_global_file(rv$unziped_files, rv$run_name, "genes_hist")
          rv$multiple_testing_correction_file <- get_global_file(rv$unziped_files, rv$run_name, "ews_obs_null_hist")
          rv$suprema_histogram_file <- get_global_file(rv$unziped_files, rv$run_name, "suprema_hist")
          rv$suprema_scatter_file <- get_global_file(rv$unziped_files, rv$run_name, "suprema_scatter")
          rv$umap_all_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_all_scatter")
          rv$umap_hypG_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_hypG_pvalue_0.05_scatter")
          rv$umap_KStest_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_sig_KStest_scatter")
          rv$umap_sig001_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_sig_0.01_scatter")
          rv$umap_sig005_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_sig_0.05_scatter")
          setProgress(message = "Results unzipped", value = 1)
        },
        detail = "This just will take a few seconds"
        )
      }else{
        rv$unziped_files <- folderTo
        rv$global_correlations_file <- get_global_file(rv$unziped_files, rv$run_name, "corr_hist")
        rv$global_correlations_rank_file <- get_global_file(rv$unziped_files, rv$run_name, "corr_rank_dist")
        rv$global_genes_file <- get_global_file(rv$unziped_files, rv$run_name, "genes_hist")
        rv$multiple_testing_correction_file <- get_global_file(rv$unziped_files, rv$run_name, "ews_obs_null_hist")
        rv$suprema_histogram_file <- get_global_file(rv$unziped_files, rv$run_name, "suprema_hist")
        rv$suprema_scatter_file <- get_global_file(rv$unziped_files, rv$run_name, "suprema_scatter")
        rv$umap_all_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_all_scatter")
        rv$umap_hypG_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_hypG_pvalue_0.05_scatter")
        rv$umap_KStest_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_sig_KStest_scatter")
        rv$umap_sig001_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_sig_0.01_scatter")
        rv$umap_sig005_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_sig_0.05_scatter")
      }



    }else{
      output$importSideControlUI <- renderUI({
        tagList(
          ##############################################
          p("pCtSEA generates a ", tags$b("zip file"), " with all the files generated by the analysis."),
          p("Here you can upload that zip file and it will be imported to show the results"),
          ##############################################
        )
      })
      output$importControlUI <- renderUI({
        #########################################
        tagList(
          fluidRow(
            column(width = 12,
                   p("Click on ", tags$b(tags$i('Browse')), " to select and import your results zipped file.")
            )
          ),
          fluidRow(
            column(width = 12,
                   textOutput(outputId = "inputDataError")
            )
          ),

          fluidRow(
            column(width = 6,
                   wellPanel(
                     fileInput(inputId = "inputUploadedFile", label = "Upload your pCtSEA results (Zip file)", multiple = FALSE)
                   )
            )
          )
        )

        #####################################
      })
    }
  }) # end of observe


  output$analysis_url <- renderUI({
    tagList("Go back to PCTSEA Analysis page: ", a("http://pctsea.scripps.edu/analyze", href="http://pctsea.scripps.edu/analyze"))
  }
  )


  rv <- reactiveValues(errorMessage="",
                       scoresCalculationsTable=NULL,
                       unziped_files=NULL,
                       run_name=NULL,
                       correlations_table=NULL,
                       global_correlations_file=NULL,
                       global_genes_file=NULL,
                       multiple_testing_correction_file=NULL,
                       suprema_histogram_file = NULL,
                       suprema_scatter_file = NULL,
                       umap_all_file = NULL,
                       umap_hypG_file = NULL,
                       umap_KStest_file = NULL,
                       umap_sig001_file = NULL,
                       umap_sig005_file = NULL,
                       parameters_file = NULL
  )
  source("./server/Table.R", local=TRUE)
  source("./server/Correlations.R", local=TRUE)
  source("./server/Scores.R", local=TRUE)
  source("./server/Genes.R", local=TRUE)
  source("./server/GlobalCorrelations.R", local=TRUE)
  source("./server/GlobalGenes.R", local=TRUE)
  source("./server/MultipleTestingCorrection.R", local=TRUE)
  source("./server/Suprema.R", local=TRUE)
  source("./server/Umap.R", local=TRUE)
  source("./server/Help.R", local=TRUE)

  output$data_loaded <- reactive({FALSE})
  outputOptions(output, "data_loaded", suspendWhenHidden = FALSE)

  # get files by uploading them and unzip them
  observeEvent(input$inputUploadedFile, {
    file <- input$inputUploadedFile
    tmp <- tools::file_path_sans_ext(basename(file$name))
    rv$run_name <- str_extract(tmp, '[^_]+$') # extract every after the last '_' until the end

    zipfilepath = file$datapath
    withProgress({
      setProgress(message = "Receiving file...", value = 0)
      # copy file to data
      newZipFilepath <- paste("data/", file$name, sep = "")
      # gets moved as temporally name at data/
      file.move(files = c(zipfilepath) , destinations = "data/", overwrite = TRUE)
      # now we need to rename it to its original name
      file.rename(from = paste0("data/", basename(zipfilepath)), to = newZipFilepath)

      folderTo <- paste0("data/", tools::file_path_sans_ext(basename(file$name)))
      # only unzip if the folder doesn't exist
      if(!file.exists(folderTo)){
        setProgress(message = "Unzipping results...", value = 0.1)
        unzip(newZipFilepath, exdir = "data")
      }

      setProgress(message = "Results unzipped", value = 1)
    },
    detail = "This just will take a few seconds"
    )
    rv$unziped_files <- folderTo
    # global files:
    rv$global_correlations_file <- get_global_file(rv$unziped_files, rv$run_name, "corr_hist")
    rv$global_correlations_rank_file <- get_global_file(rv$unziped_files, rv$run_name, "corr_rank_dist")
    rv$global_genes_file <- get_global_file(rv$unziped_files, rv$run_name, "genes_hist")
    rv$multiple_testing_correction_file <- get_global_file(rv$unziped_files, rv$run_name, "ews_obs_null_hist")
    rv$suprema_histogram_file <- get_global_file(rv$unziped_files, rv$run_name, "suprema_hist")
    rv$suprema_scatter_file <- get_global_file(rv$unziped_files, rv$run_name, "suprema_scatter")
    rv$umap_all_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_all_scatter")
    rv$umap_hypG_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_hypG_pvalue_0.05_scatter")
    rv$umap_KStest_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_sig_KStest_scatter")
    rv$umap_sig001_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_sig_0.01_scatter")
    rv$umap_sig005_file <- get_global_file(rv$unziped_files, rv$run_name, "umap_sig_0.05_scatter")

    # jump to enrichment table
    updateTabsetPanel(session, "tabs", selected = "Enrichment Table")
  })




  # select the scores file
  # scores_file <- eventReactive(rv$unziped_files,{
  #   browser()
  #   folder <- rv$unziped_files
  #   paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*score_calculations.txt")[1], sep = "")
  # })
  # read the file
  # scores_table <- eventReactive(scores_file(),{
  #     withProgress({
  #         setProgress(message = "Unzipping results...", value = 0)
  #          table = read.csv(file = scores_file(), header = TRUE, sep = "\t")
  #     })
  # })


  # select the parameters file
  observeEvent(rv$unziped_files,{
    folder <- rv$unziped_files
    # folder <-list.dirs(folder, recursive = FALSE)[1] # go one folder up
    files <- list.files(folder, pattern = ".*parameters.txt")
    if(length(files) > 0){
      file <- paste(folder, .Platform$file.sep, files[1], sep = "")
      rv$parameters_file <- file
    }
  })

  observeEvent(rv$parameters_file,{
    output$inputParametersText <- renderText({
      file <- rv$parameters_file
      all_content <- readLines(file)
      splitText <- stringi::stri_split(str = all_content, regex = '\\n')
      ret <- ""
      for(str in splitText){
        ret <- paste0(ret, "\n", str)
      }
      # browser()
      ret
      # replacedText <- lapply(splitText, p)
      # replacedText
    })
  })
  output$inputDataError <- renderText(rv$errorMessage)


}

# Run the application
shinyApp(ui = ui, server = server)#, options = ( launch.browser = TRUE))



