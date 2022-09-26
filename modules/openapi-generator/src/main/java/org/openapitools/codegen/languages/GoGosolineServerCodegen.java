package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.features.*;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class GoGosolineServerCodegen extends AbstractGoCodegen implements CodegenConfig {
    private final Logger LOGGER = LoggerFactory.getLogger(GoServerCodegen.class);

    protected String packageVersion = "1.0.0";
    protected int serverPort = 8080;
    protected String projectName = "openapi-server";
    protected String sourceFolder = "api";
    protected Boolean corsFeatureEnabled = false;
    protected Boolean addResponseHeaders = false;
    protected Boolean onlyInterfaces = false;


    public GoGosolineServerCodegen() {
        super();

        modifyFeatureSet(features -> features
                .includeDocumentationFeatures(DocumentationFeature.Readme)
                .wireFormatFeatures(EnumSet.of(WireFormatFeature.JSON, WireFormatFeature.XML))
                .securityFeatures(EnumSet.noneOf(
                        SecurityFeature.class
                ))
                .excludeGlobalFeatures(
                        GlobalFeature.XMLStructureDefinitions,
                        GlobalFeature.Callbacks,
                        GlobalFeature.LinkObjects,
                        GlobalFeature.ParameterStyling
                )
                .excludeSchemaSupportFeatures(
                        SchemaSupportFeature.Polymorphism
                )
                .excludeParameterFeatures(
                        ParameterFeature.Cookie
                )
        );

        // set the output folder here
        outputFolder = "api";

        cliOptions.add(new CliOption(CodegenConstants.SOURCE_FOLDER, CodegenConstants.SOURCE_FOLDER_DESC)
                .defaultValue(sourceFolder));

        CliOption optServerPort = new CliOption("serverPort", "The network port the generated server binds to");
        optServerPort.setType("int");
        optServerPort.defaultValue(Integer.toString(serverPort));
        cliOptions.add(optServerPort);

        CliOption optFeatureCORS = new CliOption("featureCORS", "Enable Cross-Origin Resource Sharing middleware");
        optFeatureCORS.setType("bool");
        optFeatureCORS.defaultValue(corsFeatureEnabled.toString());
        cliOptions.add(optFeatureCORS);

        cliOptions.add(CliOption.newBoolean(CodegenConstants.ENUM_CLASS_PREFIX, CodegenConstants.ENUM_CLASS_PREFIX_DESC));

        // option to include headers in the response
        CliOption optAddResponseHeaders = new CliOption("addResponseHeaders", "To include response headers in ImplResponse");
        optAddResponseHeaders.setType("bool");
        optAddResponseHeaders.defaultValue(addResponseHeaders.toString());
        cliOptions.add(optAddResponseHeaders);


        // option to exclude service factories; only interfaces are rendered
        CliOption optOnlyInterfaces = new CliOption("onlyInterfaces", "Exclude default service creators from output; only generate interfaces");
        optOnlyInterfaces.setType("bool");
        optOnlyInterfaces.defaultValue(onlyInterfaces.toString());
        cliOptions.add(optOnlyInterfaces);

        /*
         * Models.  You can write model files using the modelTemplateFiles map.
         * if you want to create one template for file, you can do so here.
         * for multiple files for model, just put another entry in the `modelTemplateFiles` with
         * a different extension
         */
        modelTemplateFiles.put(
                "model.mustache",
                ".go");
        /*
         * handler templates.  You can write services for each Api file with the apiTemplateFiles map.
            These handlers are skeletons built to implement the logic of your api using the
            expected parameters and response.
         */
        apiTemplateFiles.put(
                "handler.mustache",   // the template to use
                "_handler.go");       // the extension for each file to write

        /*
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        embeddedTemplateDir = templateDir = "go-gosoline";

        /*
         * Reserved words.  Override this with reserved words specific to your language
         */
        setReservedWordsLowerCase(
                Arrays.asList(
                        // data type
                        "string", "bool", "uint", "uint8", "uint16", "uint32", "uint64",
                        "int", "int8", "int16", "int32", "int64", "float32", "float64",
                        "complex64", "complex128", "rune", "byte", "uintptr",

                        "break", "default", "func", "interface", "select",
                        "case", "defer", "go", "map", "struct",
                        "chan", "else", "goto", "package", "switch",
                        "const", "fallthrough", "if", "range", "type",
                        "continue", "for", "import", "return", "var", "error", "nil")
                // Added "error" as it's used so frequently that it may as well be a keyword
        );
    }

    @Override
    public void processOpts() {
        super.processOpts();
        /*
         * Additional Properties.  These values can be passed to the templates and
         * are available in models, apis, and supporting files
         */
        if (additionalProperties.containsKey(CodegenConstants.PACKAGE_NAME)) {
            setPackageName((String) additionalProperties.get(CodegenConstants.PACKAGE_NAME));
        } else {
            setPackageName("openapi");
            additionalProperties.put(CodegenConstants.PACKAGE_NAME, packageName);
        }

        if (additionalProperties.containsKey(CodegenConstants.PACKAGE_VERSION)) {
            this.setPackageVersion((String) additionalProperties.get(CodegenConstants.PACKAGE_VERSION));
        } else {
            additionalProperties.put(CodegenConstants.PACKAGE_VERSION, packageVersion);
        }

        if (additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            this.setSourceFolder((String) additionalProperties.get(CodegenConstants.SOURCE_FOLDER));
        } else {
            additionalProperties.put(CodegenConstants.SOURCE_FOLDER, sourceFolder);
        }

        if (additionalProperties.containsKey("serverPort") && additionalProperties.get("serverPort") instanceof Integer) {
            this.setServerPort((int) additionalProperties.get("serverPort"));
        } else if (additionalProperties.containsKey("serverPort") && additionalProperties.get("serverPort") instanceof String) {
            try {
                this.setServerPort(Integer.parseInt(additionalProperties.get("serverPort").toString()));
                additionalProperties.put("serverPort", serverPort);
            } catch (NumberFormatException e) {
                LOGGER.warn("serverPort is not a valid integer... defaulting to {}", serverPort);
                additionalProperties.put("serverPort", serverPort);
            }
        } else {
            additionalProperties.put("serverPort", serverPort);
        }

        if (additionalProperties.containsKey("featureCORS")) {
            this.setFeatureCORS(convertPropertyToBooleanAndWriteBack("featureCORS"));
        } else {
            additionalProperties.put("featureCORS", corsFeatureEnabled);
        }

        if (additionalProperties.containsKey("addResponseHeaders")) {
            this.setAddResponseHeaders(convertPropertyToBooleanAndWriteBack("addResponseHeaders"));
        } else {
            additionalProperties.put("addResponseHeaders", addResponseHeaders);
        }

        if (additionalProperties.containsKey("onlyInterfaces")) {
            this.setOnlyInterfaces(convertPropertyToBooleanAndWriteBack("onlyInterfaces"));
        } else {
            additionalProperties.put("onlyInterfaces", onlyInterfaces);
        }

        if (this.onlyInterfaces) {
            apiTemplateFiles.remove("handler.mustache");
        }

        if (additionalProperties.containsKey(CodegenConstants.ENUM_CLASS_PREFIX)) {
            setEnumClassPrefix(Boolean.parseBoolean(additionalProperties.get(CodegenConstants.ENUM_CLASS_PREFIX).toString()));
            if (enumClassPrefix) {
                additionalProperties.put(CodegenConstants.ENUM_CLASS_PREFIX, true);
            }
        }

        modelPackage = packageName;
        apiPackage = packageName;

        supportingFiles.add(new SupportingFile("routers.mustache", sourceFolder, "routers.go"));
        supportingFiles.add(new SupportingFile("api.mustache", sourceFolder, "api.go"));
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);
        OperationMap objectMap = objs.getOperations();
        List<CodegenOperation> operations = objectMap.getOperation();

        List<Map<String, String>> imports = objs.getImports();
        if (imports == null)
            return objs;

        // override imports to only include packages for interface parameters
        imports.clear();

        boolean addedTimeImport = false;
        boolean addedOSImport = false;
        for (CodegenOperation operation : operations) {
            if (operation.vendorExtensions.containsKey("x-goso-crud"))  {
                String key = (String) operation.vendorExtensions.get("x-goso-crud");
                operation.vendorExtensions.put(String.format("x-goso-crud-is-%s", key.toLowerCase()), true);
            } else {
                operation.vendorExtensions.put("x-goso-crud-is-none", true);
            }

            for (CodegenParameter param : operation.allParams) {
                // import "os" if the operation uses files
                if (!addedOSImport && ("*os.File".equals(param.dataType) || ("[]*os.File".equals(param.dataType)))) {
                    imports.add(createMapping("import", "os"));
                    addedOSImport = true;
                }

                // import "time" if the operation has a required time parameter
                if (param.required) {
                    if (!addedTimeImport && "time.Time".equals(param.dataType)) {
                        imports.add(createMapping("import", "time"));
                        addedTimeImport = true;
                    }
                }
            }
        }

        return objs;
    }

    @Override
    public String apiPackage() {
        return sourceFolder;
    }

    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see org.openapitools.codegen.CodegenType
     */
    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    /**
     * Configures a friendly name for the generator.  This will be used by the generator
     * to select the library with the -g flag.
     *
     * @return the friendly name for the generator
     */
    @Override
    public String getName() {
        return "go-gosoline";
    }

    /**
     * Returns human-friendly help for the generator.  Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    @Override
    public String getHelp() {
        return "Generates a Go server library using OpenAPI-Generator. By default, " +
                "it will also generate handler structs -- which you can disable with the `-Dnohandler` environment variable.";
    }

    /**
     * Location to write api files.  You can use the apiPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + File.separator + apiPackage().replace('.', File.separatorChar);
    }

    public void setSourceFolder(String sourceFolder) {
        this.sourceFolder = sourceFolder;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setFeatureCORS(Boolean featureCORS) {
        this.corsFeatureEnabled = featureCORS;
    }

    public void setAddResponseHeaders(Boolean addResponseHeaders) {
        this.addResponseHeaders = addResponseHeaders;
    }

    public void setOnlyInterfaces(Boolean onlyInterfaces) {
        this.onlyInterfaces = onlyInterfaces;
    }


    @Override
    protected void updateModelForObject(CodegenModel m, Schema schema) {
        /**
         * we have a custom version of this function so we only set isMap to true if
         * ModelUtils.isMapSchema
         * In other generators, isMap is true for all type object schemas
         */
        if (schema.getProperties() != null || schema.getRequired() != null && !(schema instanceof ComposedSchema)) {
            // passing null to allProperties and allRequired as there's no parent
            addVars(m, unaliasPropertySchema(schema.getProperties()), schema.getRequired(), null, null);
        }
        if (ModelUtils.isMapSchema(schema)) {
            // an object or anyType composed schema that has additionalProperties set
            addAdditionPropertiesToCodeGenModel(m, schema);
        } else {
            m.setIsMap(false);
            if (ModelUtils.isFreeFormObject(openAPI, schema)) {
                // non-composed object type with no properties + additionalProperties
                // additionalProperties must be null, ObjectSchema, or empty Schema
                addAdditionPropertiesToCodeGenModel(m, schema);
            }
        }
        // process 'additionalProperties'
        setAddProps(schema, m);
    }
}
