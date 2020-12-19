#
# This is a Shiny web application. You can run the application by clicking
# the 'Run App' button above.
#
# Find out more about building applications with Shiny here:
#
#    http://shiny.rstudio.com/
#

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
)

# load("./data/alldata.Rdata")

# Define UI for application that draws a histogram
ui <- fluidPage(title = "PCTSEA",
                fluidRow(
                  column(12, h2("pCtSEA results explorer"))
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
                                     tabPanel("Import data",
                                              br(),

                                              uiOutput(outputId = "importSideControlUI"),

                                              br(),
                                              uiOutput(outputId = "importControlUI"),
                                     ),
                                     tabPanel("Enrichment Table",
                                              br(),
                                              p("Here you have the main output table"),
                                              fluidRow(
                                                column(width = 12,
                                                       DT::dataTableOutput(outputId = "enrichmentDataTable"), style = "font-size:80%; rowHeight: 75%"
                                                )
                                              )
                                     ),
                                     tabPanel("Global charts",
                                              br(),
                                              fluidRow(
                                                column(3, plotlyOutput(outputId = "globalCorrelationsPlot", height = "300px")),
                                                column(3, plotlyOutput(outputId = "globalCorrelationsRankPlot", height = "300px")),
                                                column(3, plotlyOutput(outputId = "globalGenesPerCellTypePlot", height = "300px"))
                                              ),
                                              fluidRow(
                                                column(3, plotlyOutput(outputId = "globalMultipleTestingCorrectionPlot", height = "300px")),
                                                column(3, plotlyOutput(outputId = "globalSupremaHistogramPlot", height = "300px")),
                                                column(3, plotlyOutput(outputId = "globalSupremaScatterPlot", height = "300px"))
                                              )
                                     ),
                                     tabPanel("Charts per cell type",
                                              br(),
                                              fluidRow(
                                                column(4, selectInput(inputId = "selectCellType", label = "Select cell type", choices = c()))

                                              ),
                                              fluidRow(
                                                column(3, plotlyOutput(outputId = "cellTypeCorrelationsPlot", height = "300px")),
                                                column(3, plotlyOutput(outputId = "cellTypeScoreCalculationPlot", height = "300px")),
                                                column(3, plotlyOutput(outputId = "genesPerCellTypePlot", height = "300px"))
                                              ),
                                              fluidRow(
                                                column(width = 6,
                                                       DT::dataTableOutput(outputId = "enrichmentDataTable2"), style = "font-size:80%; rowHeight: 75%"
                                                )
                                              )
                                     ),
                                     tabPanel("Clustering",
                                              br(),
                                              splitLayout(
                                                cellWidths = c("30%", "70%"),
                                                fluidRow(
                                                  column(12, DT::dataTableOutput(outputId = "enrichmentDataTableForCluster"), style = "font-size:80%; rowHeight: 75%")
                                                ),
                                                verticalLayout(
                                                  fluidRow(
                                                    column(4, plotlyOutput(outputId = "umapAllPlot", height = "300px")),
                                                    column(4, plotlyOutput(outputId = "umapHypGPlot", height = "300px")),
                                                    column(4, plotlyOutput(outputId = "umapKSTestPlot", height = "300px"))
                                                  ),
                                                  fluidRow(
                                                    column(4, plotlyOutput(outputId = "umapSig001Plot", height = "300px")),
                                                    column(4, plotlyOutput(outputId = "umapSig005Plot", height = "300px"))
                                                  )
                                                )
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
      inputFileName <- query$results
      tmp <- tools::file_path_sans_ext(basename(inputFileName))
      rv$run_name <- sub("_results.*", "", tmp)
      # input file should be in data folder
      zipfilepath = paste('data/', inputFileName, sep = "")

      # check if the file exist
      if (!file.exists(zipfilepath)){
        output$importSideControlUI <- renderUI({
          tagList(
            fluidRow(
              column(width = 12, h4("Opps!"), align='center')
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

      # side panel
      output$importSideControlUI <- renderUI({
        tagList(
          wellPanel(

            h5("Analysis from ptcSEA run:"),
            h5(tags$b(rv$run_name)))

        )
      })
      url <- paste(session$clientData$url_protocol, "//", session$clientData$url_hostname, ":", session$clientData$url_port, session$clientData$url_pathname, sep = "")
      output$importControlUI <- renderUI({
        tagList(
          p("Your dataset is imported in the pCtSEA results viewer. This URL will only be valid a limited time because the results will only be kept for ."),

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
      browser()
      # unzip if not already unziped
      folderTo <- paste(dirname(zipfilepath), "/", tools::file_path_sans_ext(basename(zipfilepath)), sep = "")
      if (!file.exists(folderTo)){
        withProgress({
          browser()
          setProgress(message = "Unzipping results...", value = 0)
          unzip(zipfilepath, exdir = folderTo)
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
          p("PcTSEA generates a zip file with all the output files compiled together."),
          p("Here you can upload that zip file and it will be imported to show the results"),
          p("After uploading the file, click on 'Import'")
          ##############################################
        )
      })
      output$importControlUI <- renderUI({
        #########################################
        tagList(
          fluidRow(
            column(width = 12,
                   p("Click on ", tags$b(tags$i('Browse')), " to select your results zipped file and then click on ", tags$b(tags$i("Import")), " to process it.")
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
          ),
          fluidRow(
            column(width = 1, actionButton(inputId = "importButton", label = "Import")),
            column(width = 11, textOutput(outputId = "inputDataStatus"))
          )
        )

        #####################################
      })
    }
  }) # end of observe




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
                       umap_sig005_file = NULL
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

  output$data_loaded <- reactive({FALSE})
  outputOptions(output, "data_loaded", suspendWhenHidden = FALSE)

  # get files by uploading them and unzip them
  observeEvent(input$inputUploadedFile, {
    file <- input$inputUploadedFile
    tmp <- tools::file_path_sans_ext(basename(file$name))
    rv$run_name <- sub("_results.*", "", tmp)

    zipfilepath = file$datapath
    withProgress({
      setProgress(message = "Receiving file...", value = 0)
      # copy file to data
      newZipFilepath <- paste("data/", file$name, sep = "")
      file.move(files = c(zipfilepath) , destinations = "data/", overwrite = TRUE)
      # file.rename(from = paste("data/", basename(zipfilepath), sep = ""), to = newZipFilepath)
      folderTo <- paste("data/", tools::file_path_sans_ext(basename(file$name)), sep = "")


      setProgress(message = "Unzipping results...", value = 0.1)
      unzip(newZipFilepath, exdir = folderTo)
      setProgress(message = "Unzipping results...", value = 0.5)


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


  output$inputDataError <- renderText(rv$errorMessage)





  # output$cellTypeCorrelationsPlot <- renderPlot(createPlotWithCorrelations(isolate({rv$correlationsTable}), 0.1, input$selectCellType))

  # output$cellTypeScoreCalculationPlot <- renderPlot(createPlotWithScoreCalculation(isolate({rv$scoresCalculationsTable}), input$selectCellType))


}



# Run the application
shinyApp(ui = ui, server = server, options = ( launch.browser = TRUE))



